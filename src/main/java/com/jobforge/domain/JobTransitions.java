package com.jobforge.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** The legal status changes of a job. Single source of truth. */
public final class JobTransitions {
    private static final Map<JobStatus, Set<JobStatus>> ALLOWED = Map.of(
            JobStatus.QUEUED,
            EnumSet.of(JobStatus.RUNNING, JobStatus.CANCELLED),
            JobStatus.RUNNING,
            EnumSet.of(JobStatus.SUCCEEDED, JobStatus.QUEUED, JobStatus.DEAD),
            JobStatus.DEAD,
            EnumSet.of(JobStatus.QUEUED),
            JobStatus.SUCCEEDED,
            EnumSet.noneOf(JobStatus.class),
            JobStatus.CANCELLED,
            EnumSet.noneOf(JobStatus.class));

    private JobTransitions() {
    }

    public static boolean canTransition(JobStatus from, JobStatus to) {
        return ALLOWED.get(from).contains(to);
    }
}
