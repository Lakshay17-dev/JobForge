package com.jobforge.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobforge.domain.Job;
import com.jobforge.repository.JobRepository;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jobforge.worker.enabled", havingValue = "true", matchIfMissing = true)
public class WorkerPool implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);
    private final JobRepository repo;
    private final HandlerRegistry registry;
    private final ObjectMapper mapper;
    private final WorkerProperties props;
    private final String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    private volatile boolean running;
    private Thread poller;

    public WorkerPool(JobRepository repo, HandlerRegistry registry,
            ObjectMapper mapper, WorkerProperties props) {
        this.repo = repo;
        this.registry = registry;
        this.mapper = mapper;
        this.props = props;
    }

    @Override
    public void start() {
        running = true;
        poller = new Thread(this::pollLoop, "jobforge-poller");
        poller.start();
        log.info("Started {} (poll={}ms, lease={}s)", workerId,
                props.pollIntervalMs(), props.leaseSeconds());
    }

    @Override
    public void stop() {
        running = false;
        poller.interrupt();
        try {
            poller.join(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Stopped {}", workerId);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void pollLoop() {
        while (running) {
            try {
                Optional<Job> claimed = repo.claimNext(workerId, props.leaseSeconds());
                if (claimed.isPresent()) {
                    execute(claimed.get());
                } else {
                    Thread.sleep(props.pollIntervalMs());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Polling failed: {}", e.toString());
                sleepQuietly(1000);
            }
        }
    }

    private void execute(Job job) {
        log.info("Running job {} type={} attempt={}", job.id(), job.type(),
                job.attempts());
        try {
            JobHandler handler = registry.find(job.type()).orElseThrow(() -> new IllegalArgumentException(
                    "No handler for type " + job.type()));
            handler.handle(mapper.readTree(job.payload()));
            repo.markSucceeded(job.id(), workerId);
            log.info("Job {} succeeded", job.id());
        } catch (Exception e) {
            log.warn("Job {} failed: {}", job.id(), e.toString());
            // Week 4 replaces this with retry logic.
            repo.markDead(job.id(), workerId, e.toString());
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
