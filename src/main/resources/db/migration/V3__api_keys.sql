CREATE TABLE api_keys (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    key_hash TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE jobs
ADD CONSTRAINT fk_jobs_api_key FOREIGN KEY (api_key_id) REFERENCES api_keys (id);
DROP INDEX uq_jobs_idem;
CREATE UNIQUE INDEX uq_jobs_idem ON jobs (api_key_id, idempotency_key)
WHERE idempotency_key IS NOT NULL;