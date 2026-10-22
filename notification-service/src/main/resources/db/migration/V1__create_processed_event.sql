-- One row per ClaimSubmitted event that has been emailed. The primary key is what makes the
-- consumer idempotent: a redelivered event finds its row and is skipped.
CREATE TABLE processed_event (
    event_id      UUID         PRIMARY KEY,
    claim_number  VARCHAR(13)  NOT NULL,
    processed_at  TIMESTAMPTZ  NOT NULL
);
