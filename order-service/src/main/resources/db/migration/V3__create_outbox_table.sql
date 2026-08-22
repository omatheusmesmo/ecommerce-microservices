-- Transactional Outbox: rows written in the same tx as the aggregate change,
-- captured by Debezium and published to Kafka.
CREATE SEQUENCE outbox_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE outbox (
    id BIGINT NOT NULL DEFAULT nextval('outbox_seq'),
    event_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
);

ALTER SEQUENCE outbox_seq OWNED BY outbox.id;

CREATE INDEX idx_outbox_created_at ON outbox(created_at);
CREATE UNIQUE INDEX idx_outbox_event_id ON outbox(event_id);
