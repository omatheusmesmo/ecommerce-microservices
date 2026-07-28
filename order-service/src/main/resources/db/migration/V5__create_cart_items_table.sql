CREATE SEQUENCE cart_items_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE cart_items (
    id BIGINT NOT NULL DEFAULT nextval('cart_items_seq'),
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    cart_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id)
        REFERENCES carts(id) ON DELETE CASCADE
);

ALTER SEQUENCE cart_items_seq OWNED BY cart_items.id;

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);
