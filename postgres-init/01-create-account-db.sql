-- Create Account Service Database
CREATE DATABASE banking_accounts;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE banking_accounts TO postgres;

-- Connect to the database
\c banking_accounts;

-- Create schema
CREATE SCHEMA IF NOT EXISTS public;

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO postgres;