
package com.jobforge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobforge.service.ApiKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "jobforge.worker.enabled=false",
        "jobforge.rate-limit.capacity=3",
        "jobforge.rate-limit.refill-per-second=0.01"
})
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest extends PostgresIntegrationTest {

    private static final String BODY = "{\"type\":\"echo\",\"payload\":{\"n\":1}}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApiKeyService keys;

    @Test
    void missingKeyIsRejected() throws Exception {
        mvc.perform(post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongKeyIsRejected() throws Exception {
        mvc.perform(post("/jobs")
                .header("X-API-Key", "jf_wrong")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validKeyIsAccepted() throws Exception {
        String key = keys.create("valid").rawKey();

        mvc.perform(post("/jobs")
                .header("X-API-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void tooManyRequestsAreRateLimited() throws Exception {
        String key = keys.create("limited").rawKey();

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/jobs")
                    .header("X-API-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BODY))
                    .andExpect(status().isCreated());
        }

        mvc.perform(post("/jobs")
                .header("X-API-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1"));
    }

    @Test
    void healthEndpointNeedsNoKey() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
