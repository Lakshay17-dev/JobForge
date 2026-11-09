package com.jobforge.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jobforge.worker")
public record WorkerProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("4") int threads,
        @DefaultValue("500") long pollIntervalMs,
        @DefaultValue("60") int leaseSeconds) {
}
