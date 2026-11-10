
package com.jobforge.service;

import com.jobforge.domain.Job;
import com.jobforge.domain.JobStatus;
import com.jobforge.domain.JobTransitions;
import com.jobforge.repository.JobRepository;
import com.jobforge.web.SubmitJobRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class JobService {

    private final JobRepository repo;

    public JobService(JobRepository repo) {
        this.repo = repo;
    }

    // Result of job submission
    public record SubmitResult(Job job, boolean created) {
    }

    // Submit a new job
    public SubmitResult submit(SubmitJobRequest req, String idempotencyKey,
                               UUID apiKeyId) {

        int priority = req.priority() == null ? 0 : req.priority();
        int maxAttempts = req.maxAttempts() == null ? 5 : req.maxAttempts();
        Instant runAt = req.runAt() == null ? Instant.now() : req.runAt();

        Job job = repo.insert(
                req.type(),
                req.payload().toString(),
                priority,
                runAt,
                maxAttempts,
                idempotencyKey,
                apiKeyId
        ).orElseThrow();

        return new SubmitResult(job, true);
    }

    // Get a job by ID
    public Job get(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    // List jobs with status filter and pagination
    public List<Job> list(JobStatus status, int page, int size) {

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        return repo.list(status, safeSize, safePage * safeSize);
    }

    // Manually retry a failed job
    public Job retry(UUID id) {

        Job job = get(id);

        if (job.status() != JobStatus.DEAD) {
            throw new JobConflictException(
                    "Only DEAD jobs can be retried; this job is "
                            + job.status());
        }

        if (!repo.requeueDead(id)) {
            throw new JobConflictException(
                    "Job changed state; try again");
        }

        return get(id);
    }

    // Cancel a queued job
    public Job cancel(UUID id) {

        Job job = get(id);

        if (!JobTransitions.canTransition(
                job.status(), JobStatus.CANCELLED)) {

            throw new JobConflictException(
                    "Cannot cancel a job that is " + job.status());
        }

        if (!repo.cancelQueued(id)) {
            throw new JobConflictException(
                    "A worker claimed the job before it could be cancelled");
        }

        return get(id);
    }
}
