
package com.jobforge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobforge.domain.Job;
import com.jobforge.domain.JobStatus;
import com.jobforge.domain.JobTransitions;
import com.jobforge.repository.JobRepository;
import com.jobforge.web.SubmitJobRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class JobService {

    private final JobRepository repo;
    private final ObjectMapper mapper;

    public JobService(JobRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    // Result of job submission
    public record SubmitResult(Job job, boolean created) {
    }

    // Submit a new job or return an existing job for a duplicate request.
    public SubmitResult submit(SubmitJobRequest req, String idempotencyKey,
            UUID apiKeyId) {

        int priority = req.priority() == null ? 0 : req.priority();
        int maxAttempts = req.maxAttempts() == null ? 5 : req.maxAttempts();
        Instant runAt = req.runAt() == null ? Instant.now() : req.runAt();

        Optional<Job> inserted = repo.insert(
                req.type(),
                req.payload().toString(),
                priority,
                runAt,
                maxAttempts,
                idempotencyKey,
                apiKeyId);

        if (inserted.isPresent()) {
            return new SubmitResult(inserted.get(), true);
        }

        // An existing job was found for this idempotency key.
        Job existing = repo.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new JobConflictException(
                        "Idempotency key conflict; please retry"));

        // Compare the original request with the new request.
        try {
            boolean sameType = existing.type().equals(req.type());

            boolean samePayload = mapper.readTree(existing.payload())
                    .equals(mapper.valueToTree(req.payload()));

            if (!sameType || !samePayload) {
                throw new JobConflictException(
                        "Idempotency key was already used with a different request");
            }

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Could not compare job payloads", e);
        }

        // Same key and same request: return the original job.
        return new SubmitResult(existing, false);
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
