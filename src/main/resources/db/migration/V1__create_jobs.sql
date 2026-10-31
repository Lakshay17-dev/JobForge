CREATE TABLE jobs (
    id              UUID PRIMARY KEY,
    type            TEXT        NOT NULL,
    payload         JSONB       NOT NULL,
    status          TEXT        NOT NULL,
    priority        INT         NOT NULL DEFAULT 0,
    run_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    attempts        INT         NOT NULL DEFAULT 0,
    max_attempts    INT         NOT NULL DEFAULT 5,
    locked_by       TEXT,
    locked_until    TIMESTAMPTZ,
    last_error      TEXT,
    idempotency_key TEXT,
    api_key_id      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_jobs_status CHECK
        (status IN ('QUEUED','RUNNING','SUCCEEDED','DEAD','CANCELLED'))
);-- Fast lookup of the next runnable job (highest priority, oldest run_at).
CREATE INDEX idx_jobs_claim ON jobs (priority DESC, run_at)
    WHERE status = 'QUEUED';-- Fast lookup of jobs whose lease has expired (crashed workers).
CREATE INDEX idx_jobs_lease ON jobs (locked_until)
    WHERE status = 'RUNNING';
CREATE INDEX idx_jobs_status ON jobs (status);