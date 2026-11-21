
package com.jobforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobforge.domain.Job;
import com.jobforge.domain.JobStatus;
import com.jobforge.repository.JobRepository;
import com.jobforge.service.JobConflictException;
import com.jobforge.service.JobNotFoundException;
import com.jobforge.service.JobService;
import com.jobforge.web.SubmitJobRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JobServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JobService service;

    @Autowired
    private JobRepository repo;

    private SubmitJobRequest request() {
        return new SubmitJobRequest(
                "echo",
                new ObjectMapper().createObjectNode().put("n", 1),
                null,
                null,
                null);
    }

    @Test
    void duplicateIdempotencyKeyReturnsTheExistingJob() {
        JobService.SubmitResult first = service.submit(request(), "k1", null);
        JobService.SubmitResult second = service.submit(request(), "k1", null);

        assertTrue(first.created());
        assertFalse(second.created());
        assertEquals(first.job().id(), second.job().id());
    }

    @Test
    void queuedJobCanBeCancelled() {
        Job job = service.submit(request(), null, null).job();

        assertEquals(
                JobStatus.CANCELLED,
                service.cancel(job.id()).status());
    }

    @Test
    void runningJobCannotBeCancelled() {
        Job job = service.submit(request(), null, null).job();
        repo.claimNext("w1", 60);

        assertThrows(
                JobConflictException.class,
                () -> service.cancel(job.id()));
    }

    @Test
    void onlyDeadJobsCanBeRetried() {
        Job job = service.submit(request(), null, null).job();

        assertThrows(
                JobConflictException.class,
                () -> service.retry(job.id()));
    }

    @Test
    void unknownJobIsNotFound() {
        assertThrows(
                JobNotFoundException.class,
                () -> service.get(UUID.randomUUID()));
    }
}
