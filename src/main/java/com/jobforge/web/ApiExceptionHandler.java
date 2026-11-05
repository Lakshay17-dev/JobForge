package com.jobforge.web;
import com.jobforge.service.JobConflictException;
import com.jobforge.service.JobNotFoundException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ApiError> notFound(JobNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, "not_found", e.getMessage());
    }
    @ExceptionHandler(JobConflictException.class)
    public ResponseEntity<ApiError> conflict(JobConflictException e) {
        return build(HttpStatus.CONFLICT, "conflict", e.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "validation_failed", message);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "malformed_json",
                "Request body is missing or is not valid JSON");
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badParameter(
            MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST, "bad_parameter",
                "Invalid value for parameter '" + e.getName() + "'");
    }
    private ResponseEntity<ApiError> build(HttpStatus status, String error,
                                           String message) {
        return ResponseEntity.status(status).body(new ApiError(error, message));
    }
}
