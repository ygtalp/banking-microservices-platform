-- Create Transfer Service Database
CREATE DATABASE banking_transfers;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE banking_transfers TO postgres;

-- Connect to the database
\c banking_transfers;

-- Create schema
CREATE SCHEMA IF NOT EXISTS public;

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO postgres;