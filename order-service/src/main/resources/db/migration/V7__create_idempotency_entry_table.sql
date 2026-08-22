CREATE TABLE idempotency_entry (
    id VARCHAR(255) NOT NULL,
    fingerprint VARCHAR(255),
    in_flight SMALLINT NOT NULL,
    response BYTEA,
    lock_expires_at BIGINT NOT NULL,
    response_expires_at BIGINT,

    CONSTRAINT pk_idempotency_entry PRIMARY KEY (id)
);

CREATE INDEX idx_idempotency_entry_response_expires_at ON idempotency_entry(response_expires_at);
CREATE INDEX idx_idempotency_entry_lock_expires_at ON idempotency_entry(lock_expires_at);
