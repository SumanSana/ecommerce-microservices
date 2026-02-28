
CREATE TYPE user_status AS ENUM ('ACTIVE', 'BLOCKED', 'DELETED');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuidv7(), 
    user_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    mobile_number VARCHAR(15) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    status user_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_mobile ON users(mobile_number);
CREATE INDEX idx_users_email ON users(email);


CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


INSERT INTO roles (name, description) VALUES
('ROLE_USER', 'Buyer role'),
('ROLE_SELLER', 'Seller role'),
('ROLE_ADMIN', 'Platform administrator'),
('ROLE_SUPPORT', 'Customer support');

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

grant all PRIVILEGES on table users, roles, user_roles to identity_user;

