
package com.jobforge.metrics;

import com.jobforge.domain.JobStatus;
import com.jobforge.repository.JobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class JobMetrics {

    private final Timer duration;
    private final Counter succeeded;
    private final Counter retried;
    private final Counter dead;

    public JobMetrics(MeterRegistry registry, JobRepository repo) {
        for (JobStatus status : JobStatus.values()) {
            Gauge.builder("jobforge.jobs", () -> repo.countByStatus(status))
                    .description("Number of jobs by status")
                    .tag("status", status.name())
                    .register(registry);
        }

        this.duration = Timer.builder("jobforge.job.duration")
                .description("Time spent running job handlers")
                .register(registry);

        this.succeeded = counter(registry, "success");
        this.retried = counter(registry, "retry");
        this.dead = counter(registry, "dead");
    }

    private static Counter counter(MeterRegistry registry, String result) {
        return Counter.builder("jobforge.job.executions")
                .description("Job executions by outcome")
                .tag("result", result)
                .register(registry);
    }

    public void success(Duration took) {
        duration.record(took);
        succeeded.increment();
    }

    public void retry(Duration took) {
        duration.record(took);
        retried.increment();
    }

    public void dead(Duration took) {
        duration.record(took);
        dead.increment();
    }
}
