-- Transactional outbox: written in the same transaction as the claim,
-- relayed to RabbitMQ by OutboxRelay. published_at IS NULL means "not yet sent".
CREATE TABLE outbox_event (
    id              UUID         PRIMARY KEY,
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    payload         JSONB        NOT NULL,
    correlation_id  VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL,
    published_at    TIMESTAMPTZ,
    attempts        INT          NOT NULL DEFAULT 0,
    last_error      TEXT
);

-- The relay only ever scans unpublished rows, so index just those.
CREATE INDEX ix_outbox_event_unpublished ON outbox_event (created_at) WHERE published_at IS NULL;
