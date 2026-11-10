package com.jobforge.worker.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobforge.worker.JobHandler;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** Fails randomly. Payload: {"failureRate": 0.7}. Use 1.0 to always fail. */
@Component
public class FlakyHandler implements JobHandler {
    @Override
    public String type() {
        return "flaky";
    }

    @Override
    public void handle(JsonNode payload) {
        double rate = payload.path("failureRate").asDouble(0.7);
        if (ThreadLocalRandom.current().nextDouble() < rate) {
            throw new IllegalStateException("flaky handler failed on purpose");
        }
    }
}
