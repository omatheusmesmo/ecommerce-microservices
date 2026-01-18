-- Create sequence for orders
CREATE SEQUENCE orders_seq START WITH 1 INCREMENT BY 50;

-- Create orders table
CREATE TABLE orders (
    id BIGINT NOT NULL DEFAULT nextval('orders_seq'),
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

-- Link sequence to table (optional, for better ownership)
ALTER SEQUENCE orders_seq OWNED BY orders.id;

-- Create indexes
CREATE INDEX idx_orders_customer_email ON orders(customer_email);
CREATE INDEX idx_orders_status ON orders(status);