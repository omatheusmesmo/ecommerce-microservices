-- Address snapshot columns (nullable: orders created before this migration have no address)
ALTER TABLE orders
    ADD COLUMN shipping_street VARCHAR(255),
    ADD COLUMN shipping_number VARCHAR(20),
    ADD COLUMN shipping_complement VARCHAR(255),
    ADD COLUMN shipping_city VARCHAR(100),
    ADD COLUMN shipping_state VARCHAR(100),
    ADD COLUMN shipping_zip_code VARCHAR(20),
    ADD COLUMN shipping_country VARCHAR(100),
    ADD COLUMN billing_street VARCHAR(255),
    ADD COLUMN billing_number VARCHAR(20),
    ADD COLUMN billing_complement VARCHAR(255),
    ADD COLUMN billing_city VARCHAR(100),
    ADD COLUMN billing_state VARCHAR(100),
    ADD COLUMN billing_zip_code VARCHAR(20),
    ADD COLUMN billing_country VARCHAR(100);
