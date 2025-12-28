-- Create authentication database
CREATE DATABASE banking_auth;

-- Connect to the database
\c banking_auth;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE banking_auth TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA public TO postgres;

-- Note: Liquibase will create the tables via migrations
