-- A client-supplied key makes a submit safe to repeat.
CREATE UNIQUE INDEX uq_jobs_idem
    ON jobs (idempotency_key)
    WHERE idempotency_key IS NOT NULL;