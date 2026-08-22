CREATE SEQUENCE orders_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE orders (
    id BIGINT NOT NULL DEFAULT nextval('orders_seq'),
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    total_currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    shipping_cost DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    shipping_currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

ALTER SEQUENCE orders_seq OWNED BY orders.id;

CREATE INDEX idx_orders_customer_email ON orders(customer_email);
CREATE INDEX idx_orders_status ON orders(status);
