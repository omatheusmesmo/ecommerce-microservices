-- Create sequence for addresses
CREATE SEQUENCE addresses_seq START WITH 1 INCREMENT BY 50;

-- Create addresses table (saved addresses per user)
CREATE TABLE addresses (
    id BIGINT NOT NULL DEFAULT nextval('addresses_seq'),
    user_id BIGINT NOT NULL,
    label VARCHAR(50),
    street VARCHAR(255) NOT NULL,
    number VARCHAR(20) NOT NULL,
    complement VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Link sequence to table
ALTER SEQUENCE addresses_seq OWNED BY addresses.id;

CREATE INDEX idx_addresses_user_id ON addresses(user_id);
