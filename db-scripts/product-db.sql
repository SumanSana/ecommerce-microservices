create database product_db;

CREATE USER product_user WITH PASSWORD 'product_pass';

ALTER DATABASE product_db OWNER TO product_user;

GRANT ALL PRIVILEGES ON DATABASE product_db TO product_user;

-- 1. Brands Table
CREATE TABLE brands (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(100) UNIQUE NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 2. Categories Table with hierarchy
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(100) NOT NULL,
    parent_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 3. Products Table (The "Parent" Catalog)
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    brand_id UUID REFERENCES brands(id),
    category_id UUID REFERENCES categories(id),
    status VARCHAR(1) NOT NULL CHECK (status IN ('A', 'N')),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 4. Product Variants (The "SKUs")
CREATE TABLE product_variants (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    sku_id VARCHAR(100) UNIQUE NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    attributes JSONB,
    version INT DEFAULT 0, -- For Optimistic Locking
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 5. Outbox Table for Kafka Synchronization
CREATE TABLE outbox (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT now(),
	updated_at TIMESTAMPTZ DEFAULT now()
);

---
--- OPTIMIZED INDEXES
---

-- Standard B-tree indexes for foreign keys (Essential for Joins)
CREATE INDEX idx_products_brand_id ON products(brand_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_variants_product_id ON product_variants(product_id);

-- GIN Index for JSONB attributes (Enables fast filtering by color, size, etc.)
CREATE INDEX idx_variants_attributes ON product_variants USING GIN (attributes);

-- Partial Index for the Outbox Processor
-- This makes polling for new events extremely fast
CREATE INDEX idx_outbox_unprocessed ON outbox(created_at) WHERE processed = FALSE;


GRANT ALL PRIVILEGES ON TABLE brands, categories, products, product_variants, outbox TO product_user;

select * from outbox;