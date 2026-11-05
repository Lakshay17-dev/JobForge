package com.jobforge.web;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobforge.domain.Job;
import com.jobforge.domain.JobStatus;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.UUID;
public record JobResponse(
        UUID id,
        String type,
        JobStatus status,
        JsonNode payload,
        int priority,
        Instant runAt,
        int attempts,
        int maxAttempts,
        String lastError,
        Instant createdAt,
        Instant updatedAt) {
    public static JobResponse from(Job job, ObjectMapper mapper) {
        try {
            JsonNode payload = mapper.readTree(job.payload());
            return new JobResponse(job.id(), job.type(), job.status(), payload,
                    job.priority(), job.runAt(), job.attempts(),
                    job.maxAttempts(), job.lastError(), job.createdAt(),
                    job.updatedAt());
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
