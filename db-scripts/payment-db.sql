CREATE TYPE payment_status AS ENUM (
    'INITIATED',
    'SUCCESS',
    'FAILED',
    'REFUNDED',
    'REFUND_FAILED',
    'CANCELLED'
);

-- 4. Create the Payments Table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    order_id UUID NOT NULL UNIQUE,
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    amount NUMERIC(19, 4) NOT NULL, 
    currency VARCHAR(3) DEFAULT 'USD',
    status payment_status NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0 -- For Optimistic Locking (@Version in JPA)
);

-- 5. Performance Indexes
-- We lookup by stripe_payment_intent_id in Webhooks
CREATE INDEX idx_payments_stripe_id ON payments(stripe_payment_intent_id);
-- We lookup by order_id in processRefund (Kafka Events)
CREATE INDEX idx_payments_order_id ON payments(order_id);

GRANT ALL PRIVILEGES ON TABLE payments TO payment_user;