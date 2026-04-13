-- 1. Main Inventory Table
CREATE TABLE inventory (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    sku_id VARCHAR(255) UNIQUE NOT NULL,
    total_quantity INTEGER NOT NULL DEFAULT 0 CHECK (total_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast lookups by SKU (The most common query)
CREATE INDEX idx_inventory_sku_id ON inventory(sku_id);

-- 2. Inventory Transactions (The Ledger/Audit Log)
CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    inventory_id UUID REFERENCES inventory(id) ON DELETE CASCADE,
    sku_id VARCHAR(255) NOT NULL,
    -- Transaction metadata
    transaction_type VARCHAR(50) NOT NULL, 
    quantity_changed INTEGER NOT NULL,     
    reference_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(255),             
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transaction_sku_date ON inventory_transactions(sku_id, created_at DESC);

CREATE TABLE inventory_outbox (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    aggregate_type VARCHAR(100) NOT NULL,   -- 'INVENTORY'
    aggregate_id VARCHAR(255) NOT NULL,     -- The SKU ID
    event_type VARCHAR(100) NOT NULL,       -- 'STOCK_UPDATED'
    payload JSONB NOT NULL,                 -- The actual data for Kafka
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE    -- Null until the Kafka relay picks it up
);

CREATE INDEX idx_inventory_lookup 
ON inventory_transactions (reference_id, reference_type, transaction_type);

grant all PRIVILEGES on table inventory, inventory_transactions, inventory_outbox to inventory_user;
