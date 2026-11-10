package com.jobforge.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jobforge.retry")
public record RetryProperties(
        @DefaultValue("2") long baseDelaySeconds,
        @DefaultValue("300") long maxDelaySeconds) {
}
