-- Create sequence for order_items
CREATE SEQUENCE order_items_seq START WITH 1 INCREMENT BY 50;

-- Create order_items table
CREATE TABLE order_items (
    id BIGINT NOT NULL DEFAULT nextval('order_items_seq'),
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    order_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE
);

-- Link sequence to table
ALTER SEQUENCE order_items_seq OWNED BY order_items.id;

-- Create indexes
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);