
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
            rs.getTimestamp("updated_at").toInstant()
    );

    private final JdbcTemplate jdbc;

    public JobRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

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
                apiKeyId
        ).stream().findFirst();
    }

    public Optional<Job> findById(UUID id) {
        return jdbc.query(
                "SELECT * FROM jobs WHERE id = ?",
                MAPPER,
                id
        ).stream().findFirst();
    }

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
}
