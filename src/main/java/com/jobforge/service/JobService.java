package com.jobforge.service;
import com.jobforge.domain.Job;
import com.jobforge.repository.JobRepository;
import com.jobforge.web.SubmitJobRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
@Service
public class JobService {
    private final JobRepository repo;
    public JobService(JobRepository repo) {
        this.repo = repo;
    }
    /** Result of a submit: the job, and whether a new row was created. */
    public record SubmitResult(Job job, boolean created) {
    }
    public SubmitResult submit(SubmitJobRequest req, String idempotencyKey,
                               UUID apiKeyId) {
        int priority = req.priority() == null ? 0 : req.priority();
        int maxAttempts = req.maxAttempts() == null ? 5 : req.maxAttempts();
        Instant runAt = req.runAt() == null ? Instant.now() : req.runAt();
        Job job = repo.insert(req.type(), req.payload().toString(), priority,
                        runAt, maxAttempts, idempotencyKey, apiKeyId)
                .orElseThrow();
        return new SubmitResult(job, true);
    }
    public Job get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new JobNotFoundException(id));
    }
}
