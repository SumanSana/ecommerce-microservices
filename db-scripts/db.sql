create database identity_db;
create database product_db;
create database inventory_db;
create database order_db;
create database payment_db;

CREATE USER identity_user WITH PASSWORD 'identity_pass';
CREATE USER product_user WITH PASSWORD 'product_pass';
CREATE USER inventory_user WITH PASSWORD 'inventory_pass';
CREATE USER order_user WITH PASSWORD 'order_pass';
CREATE USER payment_user WITH PASSWORD 'payment_pass';

ALTER DATABASE identity_db OWNER TO identity_user;
ALTER DATABASE product_db OWNER TO product_user;
ALTER DATABASE inventory_db OWNER TO inventory_user;
ALTER DATABASE order_db OWNER TO order_user;
ALTER DATABASE payment_db OWNER TO payment_user;
