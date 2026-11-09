package com.jobforge.worker.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobforge.worker.JobHandler;
import org.springframework.stereotype.Component;

/** Simulates slow work. Payload: {"seconds": 5}. Capped at 300 seconds. */
@Component
public class SleepHandler implements JobHandler {
    @Override
    public String type() {
        return "sleep";
    }

    @Override
    public void handle(JsonNode payload) throws InterruptedException {
        int seconds = Math.min(payload.path("seconds").asInt(1), 300);
        Thread.sleep(seconds * 1000L);
    }
}
