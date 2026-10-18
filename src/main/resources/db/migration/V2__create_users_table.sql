CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE users(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    google_sub VARCHAR(255) NOT NULL UNIQUE ,
    -- not unique: google_sub is the identity key, so nothing requires one email per account
    email VARCHAR(255) NOT NULL ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
)