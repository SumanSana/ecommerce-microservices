CREATE TABLE orders (
    id UUID PRIMARY KEY default uuidv7(),
    customer_id UUID NOT NULL,
    customer_email VARCHAR(150) NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    shipping_address_line1 TEXT NOT NULL,
    shipping_city VARCHAR(100) NOT NULL,
    shipping_zip_code VARCHAR(20) NOT NULL,
    shipping_country VARCHAR(100) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY default uuidv7(),
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    sku_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(19, 2) NOT NULL
);

CREATE TABLE order_events (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    order_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- e.g., 'PAYMENT_RECEIVED', 'INVENTORY_ALLOCATED'
    status VARCHAR(20) NOT NULL,     -- e.g., 'SUCCESS', 'FAILURE'
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order FOREIGN KEY(order_id) REFERENCES orders(id),
    UNIQUE(order_id, event_type)     -- Prevents duplicate event processing (Idempotency)
);

CREATE TABLE outbox (
    id UUID PRIMARY KEY default uuidv7(),
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
	processed_at timestamp default null
);

GRANT ALL PRIVILEGES ON TABLE orders, order_items, order_events, outbox TO order_user;
