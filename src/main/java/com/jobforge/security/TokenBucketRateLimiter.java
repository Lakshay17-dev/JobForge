package com.jobforge.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** In-memory token bucket limiter, one bucket per key. */
public class TokenBucketRateLimiter {

    private final int capacity;
    private final double refillPerSecond;
    private final LongSupplier nanoClock;

    private final ConcurrentHashMap<Object, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int capacity, double refillPerSecond) {
        this(capacity, refillPerSecond, System::nanoTime);
    }

    /** The clock is injectable so tests can control time. */
    public TokenBucketRateLimiter(int capacity, double refillPerSecond,
            LongSupplier nanoClock) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.nanoClock = nanoClock;
    }

    /** Returns true and consumes a token if the key may proceed. */
    public boolean tryAcquire(Object key) {
        Bucket bucket = buckets.computeIfAbsent(key,
                k -> new Bucket(nanoClock.getAsLong()));
        return bucket.tryTake();
    }

    private final class Bucket {
        private double tokens = capacity;
        private long lastRefill;

        Bucket(long now) {
            this.lastRefill = now;
        }

        synchronized boolean tryTake() {
            long now = nanoClock.getAsLong();
            double elapsedSeconds = (now - lastRefill) / 1_000_000_000.0;

            tokens = Math.min(
                    capacity,
                    tokens + elapsedSeconds * refillPerSecond);

            lastRefill = now;

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }

            return false;
        }
    }
}