package com.jobforge.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jobforge.rate-limit")
public record RateLimitProperties(
        @DefaultValue("20") int capacity,
        @DefaultValue("5") double refillPerSecond) {
}