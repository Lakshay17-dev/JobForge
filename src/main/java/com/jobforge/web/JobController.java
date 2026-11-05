package com.jobforge.web;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobforge.service.JobService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/jobs")
public class JobController {
    private final JobService service;
    private final ObjectMapper mapper;
    public JobController(JobService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }
 @PostMapping
    public ResponseEntity<JobResponse> submit(
            @Valid @RequestBody SubmitJobRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey) {
        JobService.SubmitResult result =
                service.submit(request, idempotencyKey, null);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .body(JobResponse.from(result.job(), mapper));
    }
    @GetMapping("/{id}")
    public JobResponse get(@PathVariable UUID id) {
        return JobResponse.from(service.get(id), mapper);
    }
}
