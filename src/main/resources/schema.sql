-- Active: 1743959773695@@127.0.0.1@5432@buoy

CREATE TABLE IF NOT EXISTS Users (
  id VARCHAR(255) NOT NULL PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  balance DECIMAL(10, 2) NOT NULL,
  version INT
);


CREATE TABLE IF NOT EXISTS Transaction (
    id VARCHAR(255),
    transaction_type VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(255),
    purpose VARCHAR(255),
    transaction_date TIMESTAMP NOT NULL,
    version INT,
    PRIMARY KEY (id)
);

-- Categories Table
CREATE TABLE IF NOT EXISTS categories (
    id VaRCHAR(255),
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) UNIQUE NOT NULL,
    budget DECIMAL(12, 2) NOT NULL,
    version INTEGER DEFAULT 0
);