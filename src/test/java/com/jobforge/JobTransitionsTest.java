
package com.jobforge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobforge.domain.JobStatus;
import com.jobforge.domain.JobTransitions;
import org.junit.jupiter.api.Test;

class JobTransitionsTest {

    @Test
    void queuedJobCanRunOrBeCancelled() {
        assertTrue(JobTransitions.canTransition(
                JobStatus.QUEUED, JobStatus.RUNNING));
        assertTrue(JobTransitions.canTransition(
                JobStatus.QUEUED, JobStatus.CANCELLED));
        assertFalse(JobTransitions.canTransition(
                JobStatus.QUEUED, JobStatus.SUCCEEDED));
    }

    @Test
    void runningJobCannotBeCancelled() {
        assertFalse(JobTransitions.canTransition(
                JobStatus.RUNNING, JobStatus.CANCELLED));
        assertTrue(JobTransitions.canTransition(
                JobStatus.RUNNING, JobStatus.QUEUED));
    }

    @Test
    void deadJobCanOnlyBeRequeued() {
        assertTrue(JobTransitions.canTransition(
                JobStatus.DEAD, JobStatus.QUEUED));
        assertFalse(JobTransitions.canTransition(
                JobStatus.DEAD, JobStatus.RUNNING));
        assertFalse(JobTransitions.canTransition(
                JobStatus.DEAD, JobStatus.SUCCEEDED));
    }

    @Test
    void finalStatesHaveNoExit() {
        for (JobStatus target : JobStatus.values()) {
            assertFalse(JobTransitions.canTransition(
                    JobStatus.SUCCEEDED, target));
            assertFalse(JobTransitions.canTransition(
                    JobStatus.CANCELLED, target));
        }
    }
}
