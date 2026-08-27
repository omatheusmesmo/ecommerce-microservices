CREATE TABLE processed_event (
    event_id VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT pk_processed_event PRIMARY KEY (event_id)
);
