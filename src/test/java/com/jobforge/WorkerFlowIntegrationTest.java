
package com.jobforge;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobforge.domain.Job;
import com.jobforge.domain.JobStatus;
import com.jobforge.repository.JobRepository;
import com.jobforge.service.JobService;
import com.jobforge.web.SubmitJobRequest;
import com.jobforge.worker.JobHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {
        "jobforge.worker.enabled=true",
        "jobforge.worker.poll-interval-ms=100",
        "jobforge.retry.base-delay-seconds=1"
})
@DirtiesContext
class WorkerFlowIntegrationTest extends PostgresIntegrationTest {

    @TestConfiguration
    static class ExtraHandlers {

        @Bean
        JobHandler alwaysFailHandler() {
            return new JobHandler() {

                @Override
                public String type() {
                    return "always-fail";
                }

                @Override
                public void handle(JsonNode payload) {
                    throw new IllegalStateException("boom");
                }
            };
        }
    }

    @Autowired
    private JobService service;

    @Autowired
    private JobRepository repo;

    private Job submit(String type, Integer maxAttempts) {
        SubmitJobRequest request = new SubmitJobRequest(
                type,
                new ObjectMapper().createObjectNode(),
                null,
                null,
                maxAttempts);

        return service.submit(request, null, null).job();
    }

    @Test
    void echoJobIsExecutedAndSucceeds() {
        Job job = submit("echo", null);

        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> assertEquals(
                        JobStatus.SUCCEEDED,
                        repo.findById(job.id())
                                .orElseThrow()
                                .status()));
    }

    @Test
    void failingJobIsRetriedAndThenDies() {
        Job job = submit("always-fail", 2);

        await()
                .atMost(20, SECONDS)
                .untilAsserted(() -> {
                    Job current = repo.findById(job.id())
                            .orElseThrow();

                    assertEquals(JobStatus.DEAD, current.status());
                    assertEquals(2, current.attempts());
                    assertTrue(current.lastError().contains("boom"));
                });
    }
}
