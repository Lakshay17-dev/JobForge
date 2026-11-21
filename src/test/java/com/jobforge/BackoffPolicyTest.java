
package com.jobforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobforge.worker.BackoffPolicy;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class BackoffPolicyTest {

    private static final Duration BASE = Duration.ofSeconds(2);
    private static final Duration CAP = Duration.ofSeconds(60);

    @Test
    void firstAttemptWithNoJitterIsHalfTheBase() {
        assertEquals(Duration.ofSeconds(1),
                BackoffPolicy.compute(1, BASE, CAP, 0.0));
    }

    @Test
    void delayDoublesWithEachAttempt() {
        assertEquals(Duration.ofSeconds(2),
                BackoffPolicy.compute(2, BASE, CAP, 0.0));
        assertEquals(Duration.ofSeconds(4),
                BackoffPolicy.compute(3, BASE, CAP, 0.0));
    }

    @Test
    void delayIsCapped() {
        assertEquals(Duration.ofSeconds(30),
                BackoffPolicy.compute(10, BASE, CAP, 0.0));
        assertTrue(BackoffPolicy.compute(10, BASE, CAP, 0.999999)
                .compareTo(CAP) < 0);
    }

    @Test
    void jitterStaysWithinBounds() {
        for (double random : new double[] { 0.0, 0.25, 0.5, 0.99 }) {
            long millis = BackoffPolicy.compute(3, BASE, CAP, random)
                    .toMillis();
            assertTrue(millis >= 4000 && millis < 8000, "was " + millis);
        }
    }

    @Test
    void hugeAttemptNumbersDoNotOverflow() {
        long millis = BackoffPolicy.compute(1000, BASE, CAP, 0.5)
                .toMillis();
        assertTrue(millis > 0 && millis <= 60_000);
    }
}
