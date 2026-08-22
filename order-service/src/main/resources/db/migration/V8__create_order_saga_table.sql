CREATE SEQUENCE order_saga_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE order_saga (
    id BIGINT NOT NULL DEFAULT nextval('order_saga_seq'),
    order_id BIGINT NOT NULL UNIQUE,
    current_step VARCHAR(50) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    deadline_at TIMESTAMP,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_saga_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

ALTER SEQUENCE order_saga_seq OWNED BY order_saga.id;

CREATE INDEX idx_order_saga_current_step ON order_saga(current_step);
CREATE INDEX idx_order_saga_deadline_at ON order_saga(deadline_at) WHERE deadline_at IS NOT NULL;
