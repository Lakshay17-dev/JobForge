package com.jobforge.security;

import com.jobforge.service.ApiKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates a first API key on an empty database and logs it once. */
@Component
public class ApiKeyBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyBootstrap.class);

    private final ApiKeyService keys;

    public ApiKeyBootstrap(ApiKeyService keys) {
        this.keys = keys;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (keys.count() == 0) {
            ApiKeyService.CreatedKey created = keys.create("default");
            log.warn(
                    "Created initial API key (shown once): {}",
                    created.rawKey());
        }
    }
}