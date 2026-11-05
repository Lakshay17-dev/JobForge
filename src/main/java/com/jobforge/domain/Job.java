package com.jobforge.domain;
import java.time.Instant;
import java.util.UUID;
public record Job(
        UUID id,
        String type,
        String payload,
        JobStatus status,
        int priority,
        Instant runAt,
        int attempts,
        int maxAttempts,
        String lockedBy,
        Instant lockedUntil,
        String lastError,
        String idempotencyKey,
        UUID apiKeyId,
        Instant createdAt,
        Instant updatedAt) {
}
