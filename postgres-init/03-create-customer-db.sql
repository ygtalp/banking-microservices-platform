-- Create database for Customer Service
CREATE DATABASE banking_customers;

-- Connect to the database
\c banking_customers;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE banking_customers TO postgres;

-- Success message
SELECT 'Customer Service database created successfully' AS status;
