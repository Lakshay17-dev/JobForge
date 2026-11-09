package com.jobforge.worker.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobforge.worker.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EchoHandler implements JobHandler {
    private static final Logger log = LoggerFactory.getLogger(EchoHandler.class);

    @Override
    public String type() {
        return "echo";
    }

    @Override
    public void handle(JsonNode payload) {
        log.info("echo: {}", payload);
    }
}
