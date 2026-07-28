CREATE SEQUENCE carts_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE carts (
    id BIGINT NOT NULL DEFAULT nextval('carts_seq'),
    customer_email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

ALTER SEQUENCE carts_seq OWNED BY carts.id;

CREATE INDEX idx_carts_customer_email ON carts(customer_email);
CREATE INDEX idx_carts_status ON carts(status);

CREATE UNIQUE INDEX idx_carts_one_active_per_customer ON carts(customer_email) WHERE status = 'ACTIVE';
