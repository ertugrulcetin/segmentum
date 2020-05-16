CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--;;

CREATE EXTENSION IF NOT EXISTS "citext";

--;;

SET TIME ZONE 'UTC';

--;;

CREATE TABLE IF NOT EXISTS roles (
id UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL PRIMARY KEY,
name VARCHAR(50) NOT NULL,
priority INTEGER UNIQUE NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--;;

CREATE TABLE IF NOT EXISTS users (
id UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
surname VARCHAR(100) NOT NULL,
email VARCHAR(254) UNIQUE NOT NULL,
password_salt UUID UNIQUE NOT NULL,
password UUID UNIQUE NOT NULL,
role_id UUID NOT NULL REFERENCES roles(id),
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--;;

CREATE TABLE IF NOT EXISTS auth_token (
id UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL PRIMARY KEY,
token VARCHAR(200) UNIQUE NOT NULL,
user_id UUID NOT NULL REFERENCES users(id),
deleted_at TIMESTAMP,
user_agent TEXT,
ip_address VARCHAR(32),
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--;;

CREATE TABLE IF NOT EXISTS settings (
key VARCHAR(254) UNIQUE NOT NULL PRIMARY KEY,
value TEXT NOT NULL);

--;;

CREATE TABLE IF NOT EXISTS workspace (
id UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL PRIMARY KEY,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
created_by UUID NOT NULL,
name citext UNIQUE NOT NULL,
archived BOOLEAN DEFAULT false);

--;;

CREATE TABLE IF NOT EXISTS source_types (
type VARCHAR(254) UNIQUE NOT NULL PRIMARY KEY
);

--;;

INSERT INTO source_types (type) VALUES ('javascript');

--;;

CREATE TABLE IF NOT EXISTS source (
id UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL PRIMARY KEY,
workspace_id UUID NOT NULL REFERENCES workspace(id),
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
created_by UUID NOT NULL,
name citext NOT NULL,
write_key VARCHAR(254) UNIQUE NOT NULL,
type VARCHAR(254) NOT NULL REFERENCES source_types(type),
UNIQUE(workspace_id, name));

--;;

CREATE TABLE IF NOT EXISTS destination_types (
type VARCHAR(254) UNIQUE NOT NULL PRIMARY KEY);

--;;

INSERT INTO destination_types (type) VALUES ('google_analytics');

--;;

CREATE TABLE IF NOT EXISTS destination (
id UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL PRIMARY KEY,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
created_by UUID NOT NULL,
type VARCHAR(254) NOT NULL REFERENCES destination_types(type),
source_id UUID NOT NULL REFERENCES source(id) ON DELETE CASCADE,
config JSONB NOT NULL);

--;;

CREATE TABLE IF NOT EXISTS events (
id UUID UNIQUE NOT NULL PRIMARY KEY,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
write_key VARCHAR(254) UNIQUE NOT NULL,
payload JSONB NOT NULL);

--;;

CREATE INDEX idx_events ON events(write_key);

--;;

CREATE TABLE IF NOT EXISTS success_events (
id UUID UNIQUE NOT NULL PRIMARY KEY,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
write_key VARCHAR(254) UNIQUE NOT NULL,
destination_id UUID NOT NULL REFERENCES destination(id),
request_payload JSONB NOT NULL,
response JSONB NOT NULL,
meta JSONB NOT NULL);

--;;

CREATE TABLE IF NOT EXISTS fail_events (
id UUID UNIQUE NOT NULL PRIMARY KEY,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
destination_id UUID NOT NULL REFERENCES destination(id),
write_key VARCHAR(254) UNIQUE NOT NULL,
request_payload JSONB NOT NULL,
response JSONB NOT NULL,
meta JSONB NOT NULL);
