-- Active: 1743959773695@@127.0.0.1@5432@buoy
CREATE TABLE IF NOT EXISTS Transaction(
    id SERIAL NOT NULL,
    transaction_type VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    email VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    purpose VARCHAR(255),
    transaction_source VARCHAR(255),
    transaction_date TIMESTAMP NOT NULL,
    version INT,
    PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS Users(
  id SERIAL NOT NULL,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  version INT,
  PRIMARY KEY (id)
);

