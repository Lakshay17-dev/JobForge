package com.jobforge.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

    /** The raw key is only available at creation time. */
    public record CreatedKey(UUID id, String rawKey) {
    }

    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public ApiKeyService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CreatedKey create(String name) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);

        String rawKey = "jf_" + HexFormat.of().formatHex(bytes);
        UUID id = UUID.randomUUID();

        jdbc.update(
                "INSERT INTO api_keys (id, name, key_hash) VALUES (?, ?, ?)",
                id, name, hash(rawKey));

        return new CreatedKey(id, rawKey);
    }

    public Optional<UUID> authenticate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }

        return jdbc.query(
                "SELECT id FROM api_keys WHERE key_hash = ?",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                hash(rawKey)).stream().findFirst();
    }

    public long count() {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM api_keys",
                Long.class);

        return count == null ? 0 : count;
    }

    static String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}