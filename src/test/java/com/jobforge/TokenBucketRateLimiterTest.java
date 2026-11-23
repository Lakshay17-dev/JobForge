
package com.jobforge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobforge.security.TokenBucketRateLimiter;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class TokenBucketRateLimiterTest {

    private static final long ONE_SECOND = 1_000_000_000L;

    @Test
    void allowsABurstUpToCapacityThenRejects() {
        AtomicLong now = new AtomicLong();

        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 1.0, now::get);

        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));

        assertFalse(limiter.tryAcquire("a"));
    }

    @Test
    void refillsOverTime() {
        AtomicLong now = new AtomicLong();

        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 1.0, now::get);

        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a");
        }

        assertFalse(limiter.tryAcquire("a"));

        now.addAndGet(ONE_SECOND);

        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));
    }

    @Test
    void keysHaveIndependentBuckets() {
        AtomicLong now = new AtomicLong();

        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(1, 0.1, now::get);

        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));

        assertTrue(limiter.tryAcquire("b"));
    }

    @Test
    void tokensNeverExceedCapacity() {
        AtomicLong now = new AtomicLong();

        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 1.0, now::get);

        now.addAndGet(3600 * ONE_SECOND);

        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));

        assertFalse(limiter.tryAcquire("a"));
    }
}
