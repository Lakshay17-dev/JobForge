package com.jobforge.web;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
public record SubmitJobRequest(
        @NotBlank @Size(max = 100) String type,
        JsonNode payload,
        @Min(-100) @Max(100) Integer priority,
        Instant runAt,
        @Min(1) @Max(20) Integer maxAttempts) {
    @JsonIgnore
    @AssertTrue(message = "payload must be a JSON object")
    public boolean isPayloadObject() {
        return payload != null && payload.isObject();
    }
}
