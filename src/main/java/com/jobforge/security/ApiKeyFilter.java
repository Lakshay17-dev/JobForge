package com.jobforge.security;

import com.jobforge.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String API_KEY_ID = "jobforge.apiKeyId";

    private final ApiKeyService keys;
    private final TokenBucketRateLimiter limiter;

    public ApiKeyFilter(ApiKeyService keys, RateLimitProperties props) {
        this.keys = keys;
        this.limiter = new TokenBucketRateLimiter(
                props.capacity(),
                props.refillPerSecond());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/jobs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        Optional<UUID> keyId = keys.authenticate(request.getHeader("X-API-Key"));

        if (keyId.isEmpty()) {
            reject(
                    response,
                    401,
                    "unauthorized",
                    "Missing or invalid X-API-Key");
            return;
        }

        if (!limiter.tryAcquire(keyId.get())) {
            response.setHeader("Retry-After", "1");
            reject(response, 429, "rate_limited", "Too many requests");
            return;
        }

        request.setAttribute(API_KEY_ID, keyId.get());
        chain.doFilter(request, response);
    }

    private void reject(
            HttpServletResponse response,
            int status,
            String error,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");

        response.getWriter().write(
                "{\"error\":\"" + error
                        + "\",\"message\":\"" + message + "\"}");
    }
}