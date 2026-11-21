
package com.jobforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobforge.domain.Job;
import com.jobforge.domain.JobStatus;
import com.jobforge.repository.JobRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JobRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JobRepository repo;

    private Job insertJob(int priority, Instant runAt, int maxAttempts) {
        return repo.insert("echo", "{}", priority, runAt, maxAttempts,
                null, null).orElseThrow();
    }

    private Job insertJob() {
        return insertJob(0, Instant.now(), 5);
    }

    private void expireLease(UUID id) {
        jdbc.update(
                "UPDATE jobs SET locked_until = now() - interval '1 second' WHERE id = ?",
                id);
    }

    @Test
    void claimedJobIsRunningAndLeased() {
        Job job = insertJob();
        Job claimed = repo.claimNext("w1", 60).orElseThrow();

        assertEquals(job.id(), claimed.id());
        assertEquals(JobStatus.RUNNING, claimed.status());
        assertEquals(1, claimed.attempts());
        assertEquals("w1", claimed.lockedBy());
        assertNotNull(claimed.lockedUntil());
    }

    @Test
    void nothingIsClaimedFromAnEmptyQueue() {
        assertTrue(repo.claimNext("w1", 60).isEmpty());
    }

    @Test
    void futureJobsAreNotClaimedYet() {
        insertJob(0, Instant.now().plusSeconds(3600), 5);
        assertTrue(repo.claimNext("w1", 60).isEmpty());
    }

    @Test
    void higherPriorityIsClaimedFirst() {
        insertJob(0, Instant.now(), 5);
        Job high = insertJob(10, Instant.now(), 5);

        assertEquals(high.id(), repo.claimNext("w1", 60).orElseThrow().id());
    }

    @Test
    void expiredLeaseIsReclaimedByAnotherWorker() {
        Job job = insertJob();
        repo.claimNext("w1", 60);
        expireLease(job.id());

        Job again = repo.claimNext("w2", 60).orElseThrow();

        assertEquals(job.id(), again.id());
        assertEquals(2, again.attempts());
        assertEquals("w2", again.lockedBy());
    }

    @Test
    void staleWorkerCannotCompleteAJobItLost() {
        Job job = insertJob();
        repo.claimNext("w1", 60);
        expireLease(job.id());
        repo.claimNext("w2", 60);

        assertFalse(repo.markSucceeded(job.id(), "w1"));
        assertTrue(repo.markSucceeded(job.id(), "w2"));
    }

    @Test
    void exhaustedAbandonedJobIsReapedNotReclaimed() {
        Job job = insertJob(0, Instant.now(), 1);
        repo.claimNext("w1", 60);
        expireLease(job.id());

        assertTrue(repo.claimNext("w2", 60).isEmpty());
        assertEquals(1, repo.reapExhausted());
        assertEquals(JobStatus.DEAD,
                repo.findById(job.id()).orElseThrow().status());
    }

    @Test
    void duplicateIdempotencyKeyInsertsNothing() {
        Optional<Job> first = repo.insert("echo", "{}", 0, Instant.now(),
                5, "key-1", null);
        Optional<Job> second = repo.insert("echo", "{}", 0, Instant.now(),
                5, "key-1", null);

        assertTrue(first.isPresent());
        assertTrue(second.isEmpty());
        assertEquals(first.get().id(),
                repo.findByIdempotencyKey("key-1").orElseThrow().id());
    }

    @Test
    void concurrentWorkersNeverClaimTheSameJob() throws Exception {
        int jobs = 200;

        for (int i = 0; i < jobs; i++) {
            insertJob();
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        ConcurrentLinkedQueue<UUID> claimedIds = new ConcurrentLinkedQueue<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int w = 0; w < 8; w++) {
            String worker = "w" + w;

            futures.add(pool.submit(() -> {
                Optional<Job> job;
                while ((job = repo.claimNext(worker, 60)).isPresent()) {
                    claimedIds.add(job.get().id());
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }

        pool.shutdown();

        assertEquals(jobs, claimedIds.size());
        assertEquals(jobs, new HashSet<>(claimedIds).size());
    }
}
