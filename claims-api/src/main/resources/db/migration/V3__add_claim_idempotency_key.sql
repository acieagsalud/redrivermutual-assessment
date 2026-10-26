-- Client-supplied key that makes a repeated submission (a retry after a timeout, a refresh) return
-- the claim it already created instead of creating a second one. NULL for clients that send none;
-- PostgreSQL treats NULLs as distinct, so the unique index only constrains real keys.
ALTER TABLE claim ADD COLUMN idempotency_key VARCHAR(64);
CREATE UNIQUE INDEX uk_claim_idempotency_key ON claim (idempotency_key);
