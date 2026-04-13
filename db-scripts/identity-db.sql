
CREATE TYPE user_status AS ENUM ('ACTIVE', 'BLOCKED', 'DELETED');

CREATE TYPE user_roles AS ENUM ('ADMIN', 'SUPPORT', 'USER');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuidv7(), 
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    mobile VARCHAR(10) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    status user_status NOT NULL DEFAULT 'ACTIVE',
    role user_roles NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_mobile ON users(mobile);
CREATE INDEX idx_users_email ON users(email);


grant all PRIVILEGES on table users to identity_user;

