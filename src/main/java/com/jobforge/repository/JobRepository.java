
package com.jobforge.repository;

import com.jobforge.domain.Job;
import com.jobforge.domain.JobStatus;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JobRepository {

    private static final RowMapper<Job> MAPPER = (rs, rowNum) -> new Job(
            rs.getObject("id", UUID.class),
            rs.getString("type"),
            rs.getString("payload"),
            JobStatus.valueOf(rs.getString("status")),
            rs.getInt("priority"),
            rs.getTimestamp("run_at").toInstant(),
            rs.getInt("attempts"),
            rs.getInt("max_attempts"),
            rs.getString("locked_by"),
            toInstant(rs.getTimestamp("locked_until")),
            rs.getString("last_error"),
            rs.getString("idempotency_key"),
            rs.getObject("api_key_id", UUID.class),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());

    private final JdbcTemplate jdbc;

    public JobRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    // Insert a new job into the database
    public Optional<Job> insert(
            String type,
            String payloadJson,
            int priority,
            Instant runAt,
            int maxAttempts,
            String idempotencyKey,
            UUID apiKeyId) {

        String sql = """
                INSERT INTO jobs (id, type, payload, status, priority, run_at,
                                  max_attempts, idempotency_key, api_key_id)
                VALUES (?, ?, ?::jsonb, 'QUEUED', ?, ?, ?, ?, ?)
                RETURNING *
                """;

        return jdbc.query(
                sql,
                MAPPER,
                UUID.randomUUID(),
                type,
                payloadJson,
                priority,
                Timestamp.from(runAt),
                maxAttempts,
                idempotencyKey,
                apiKeyId).stream().findFirst();
    }

    // Find a job using its ID
    public Optional<Job> findById(UUID id) {
        return jdbc.query(
                "SELECT * FROM jobs WHERE id = ?",
                MAPPER,
                id).stream().findFirst();
    }

    // List jobs, optionally filtered by status
    public List<Job> list(JobStatus status, int limit, int offset) {

        if (status == null) {
            return jdbc.query("""
                    SELECT * FROM jobs
                    ORDER BY created_at DESC
                    LIMIT ? OFFSET ?
                    """, MAPPER, limit, offset);
        }

        return jdbc.query("""
                SELECT * FROM jobs
                WHERE status = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """, MAPPER, status.name(), limit, offset);
    }

    // Atomically claim the next available job
    public Optional<Job> claimNext(String workerId, int leaseSeconds) {

        String sql = """
                UPDATE jobs
                   SET status = 'RUNNING',
                       locked_by = ?,
                       locked_until = now() + (? * interval '1 second'),
                       attempts = attempts + 1,
                       updated_at = now()
                 WHERE id = (
                       SELECT id FROM jobs
                         WHERE (status = 'QUEUED' AND run_at <= now())
                            OR (status = 'RUNNING' AND locked_until < now())
                         ORDER BY priority DESC, run_at
                         FOR UPDATE SKIP LOCKED
                         LIMIT 1
                 )
                RETURNING *
                """;

        return jdbc.query(sql, MAPPER, workerId, leaseSeconds)
                .stream()
                .findFirst();
    }

    // Mark a job as successfully completed
    public boolean markSucceeded(UUID id, String workerId) {

        return jdbc.update("""
                UPDATE jobs
                   SET status = 'SUCCEEDED',
                       locked_by = NULL,
                       locked_until = NULL,
                       updated_at = now()
                 WHERE id = ?
                   AND status = 'RUNNING'
                   AND locked_by = ?
                """, id, workerId) == 1;
    }

    // Mark a job as permanently failed
    public boolean markDead(UUID id, String workerId, String error) {

        return jdbc.update("""
                UPDATE jobs
                   SET status = 'DEAD',
                       locked_by = NULL,
                       locked_until = NULL,
                       last_error = ?,
                       updated_at = now()
                 WHERE id = ?
                   AND status = 'RUNNING'
                   AND locked_by = ?
                """, error, id, workerId) == 1;
    }
}
