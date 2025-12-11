-- Create databases for each microservice
CREATE DATABASE account_db;
CREATE DATABASE transfer_db;
CREATE DATABASE transaction_db;
CREATE DATABASE analytics_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE account_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE transfer_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE transaction_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO postgres;