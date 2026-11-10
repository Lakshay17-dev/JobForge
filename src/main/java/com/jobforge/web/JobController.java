
package com.jobforge.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobforge.domain.JobStatus;
import com.jobforge.service.JobService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    // Submit a new job
    @PostMapping
    public ResponseEntity<JobResponse> submit(
            @Valid @RequestBody SubmitJobRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        JobService.SubmitResult result = service.submit(request, idempotencyKey, null);

        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status)
                .body(JobResponse.from(result.job(), mapper));
    }

    // Get a job by ID
    @GetMapping("/{id}")
    public JobResponse get(@PathVariable UUID id) {
        return JobResponse.from(service.get(id), mapper);
    }

    // List jobs with optional status filter and pagination
    @GetMapping
    public List<JobResponse> list(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return service.list(status, page, size).stream()
                .map(job -> JobResponse.from(job, mapper))
                .toList();
    }

    // Manually retry a failed job
    @PostMapping("/{id}/retry")
    public JobResponse retry(@PathVariable UUID id) {
        return JobResponse.from(service.retry(id), mapper);
    }

    // Cancel a queued job
    @DeleteMapping("/{id}")
    public JobResponse cancel(@PathVariable UUID id) {
        return JobResponse.from(service.cancel(id), mapper);
    }
}
