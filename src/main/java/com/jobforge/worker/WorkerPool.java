
package com.jobforge.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobforge.domain.Job;
import com.jobforge.repository.JobRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final BackoffPolicy backoff;

    private final String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);

    private final Semaphore slots;

    private volatile boolean running;
    private Thread poller;
    private ExecutorService executor;

    public WorkerPool(
            JobRepository repo,
            HandlerRegistry registry,
            ObjectMapper mapper,
            WorkerProperties props,
            BackoffPolicy backoff) {

        this.repo = repo;
        this.registry = registry;
        this.mapper = mapper;
        this.props = props;
        this.backoff = backoff;
        this.slots = new Semaphore(props.threads());
    }

    @Override
    public void start() {

        AtomicInteger counter = new AtomicInteger();

        executor = Executors.newFixedThreadPool(
                props.threads(),
                runnable -> new Thread(
                        runnable,
                        "jobforge-job-" + counter.incrementAndGet()));

        running = true;

        poller = new Thread(
                this::pollLoop,
                "jobforge-poller");

        poller.start();

        log.info(
                "Started {} (threads={}, poll={}ms, lease={}s)",
                workerId,
                props.threads(),
                props.pollIntervalMs(),
                props.leaseSeconds());
    }

    @Override
    public void stop() {

        running = false;
        poller.interrupt();

        executor.shutdown();

        try {
            poller.join(5_000);

            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {

                log.warn(
                        "Jobs still running after 30s; leases will recover them");

                executor.shutdownNow();
            }

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

            boolean holdingSlot = false;

            try {

                slots.acquire();
                holdingSlot = true;

                Optional<Job> claimed = repo.claimNext(workerId, props.leaseSeconds());

                if (claimed.isPresent()) {

                    Job job = claimed.get();

                    executor.execute(() -> {
                        try {
                            execute(job);
                        } finally {
                            slots.release();
                        }
                    });

                    holdingSlot = false;

                } else {

                    slots.release();
                    holdingSlot = false;

                    int reaped = repo.reapExhausted();

                    if (reaped > 0) {
                        log.warn(
                                "Marked {} abandoned job(s) as DEAD",
                                reaped);
                    }

                    Thread.sleep(props.pollIntervalMs());
                }

            } catch (InterruptedException e) {

                if (holdingSlot) {
                    slots.release();
                }

                Thread.currentThread().interrupt();
                break;

            } catch (Exception e) {

                if (holdingSlot) {
                    slots.release();
                }

                log.error("Polling failed: {}", e.toString());

                sleepQuietly(1000);
            }
        }
    }

    private void execute(Job job) {

        log.info(
                "Running job {} type={} attempt={}",
                job.id(),
                job.type(),
                job.attempts());

        try {

            JobHandler handler = registry.find(job.type())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No handler for type " + job.type()));

            handler.handle(mapper.readTree(job.payload()));

            repo.markSucceeded(job.id(), workerId);

            log.info("Job {} succeeded", job.id());

        } catch (Exception e) {

            handleFailure(job, e);
        }
    }

    private void handleFailure(Job job, Exception e) {

        String error = truncate(e.toString());

        if (job.attempts() >= job.maxAttempts()) {

            repo.markDead(job.id(), workerId, error);

            log.error(
                    "Job {} is DEAD after {} attempts: {}",
                    job.id(),
                    job.attempts(),
                    error);

        } else {

            Duration delay = backoff.delayFor(job.attempts());

            repo.markForRetry(
                    job.id(),
                    workerId,
                    delay.toMillis(),
                    error);

            log.warn(
                    "Job {} failed (attempt {}/{}), retry in {} ms: {}",
                    job.id(),
                    job.attempts(),
                    job.maxAttempts(),
                    delay.toMillis(),
                    error);
        }
    }

    private static String truncate(String text) {
        return text.length() <= 1000
                ? text
                : text.substring(0, 1000);
    }

    private void sleepQuietly(long millis) {

        try {
            Thread.sleep(millis);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
