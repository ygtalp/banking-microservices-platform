-- Create SWIFT Service Database
CREATE DATABASE banking_swift;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE banking_swift TO postgres;

-- Connect to the database
\c banking_swift;

-- Create schema
CREATE SCHEMA IF NOT EXISTS public;

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO postgres;
