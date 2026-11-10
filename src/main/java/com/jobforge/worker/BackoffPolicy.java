package com.jobforge.worker;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class BackoffPolicy {
    private final RetryProperties props;

    public BackoffPolicy(RetryProperties props) {
        this.props = props;
    }

    /** Delay before retrying after the given failed attempt (1-based). */
    public Duration delayFor(int attempt) {
        return compute(attempt,
                Duration.ofSeconds(props.baseDelaySeconds()),
                Duration.ofSeconds(props.maxDelaySeconds()),
                ThreadLocalRandom.current().nextDouble());
    }

    /**
     * Pure function (easy to unit test). random must be in [0, 1).
     * delay = min(base * 2^(attempt-1), cap), then "equal jitter":
     * half of it plus a random share of the other half.
     */
    public static Duration compute(int attempt, Duration base, Duration cap,
            double random) {
        int shift = Math.min(Math.max(attempt - 1, 0), 20);
        long raw = base.toMillis() * (1L << shift);
        long capped = Math.min(raw, cap.toMillis());
        long half = capped / 2;
        long jittered = half + (long) (random * half);
        return Duration.ofMillis(jittered);
    }
}
