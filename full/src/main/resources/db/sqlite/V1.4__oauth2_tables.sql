-- EM: modified from Michael Hanl version

-- oauth2 db tables
CREATE TABLE IF NOT EXISTS oauth2_client (
	id VARCHAR(100) PRIMARY KEY NOT NULL,
	name VARCHAR(100) NOT NULL,
	secret VARCHAR(255) DEFAULT NULL,
	type VARCHAR(50) NOT NULL,
	super BOOLEAN DEFAULT FALSE,
	redirect_uri TEXT DEFAULT NULL,
	description VARCHAR(255) NOT NULL,
	registered_by VARCHAR(100) NOT NULL,
	--url_hashcode INTEGER,	
	url TEXT DEFAULT NULL
);

--CREATE UNIQUE INDEX client_url_index on oauth2_client(url_hashcode);

CREATE TABLE IF NOT EXISTS oauth2_access_scope (
	id VARCHAR(100) PRIMARY KEY NOT NULL
);

-- authorization tables are not needed if using cache

--CREATE TABLE IF NOT EXISTS oauth2_authorization (
--	id INTEGER PRIMARY KEY AUTOINCREMENT,
--	code VARCHAR(255) NOT NULL,
--	client_id VARCHAR(100) NOT NULL,
--	user_id VARCHAR(100) NOT NULL,
--	redirect_uri TEXT DEFAULT NULL,
--	created_date TIMESTAMP NOT NULL,
--	expiry_date TIMESTAMP NOT NULL,
--	is_revoked BOOLEAN DEFAULT 0,
--	total_attempts INTEGER DEFAULT 0,
--	user_auth_time TIMESTAMP NOT NULL,
--	nonce TEXT DEFAULT NULL,
--	FOREIGN KEY (client_id)
--	   REFERENCES oauth2_client(id)
--);
--
--CREATE UNIQUE INDEX authorization_index on oauth2_authorization(code, client_id);
--
--CREATE TABLE IF NOT EXISTS oauth2_authorization_scope (
--	id INTEGER PRIMARY KEY AUTOINCREMENT,
--	authorization_id INTEGER NOT NULL,
--	scope_id VARCHAR(100) NOT NULL,
--	FOREIGN KEY (authorization_id)
--	   REFERENCES oauth2_authorization(id),
--	FOREIGN KEY (scope_id)
--	   REFERENCES oauth2_access_scope(id)
--);
--
--CREATE UNIQUE INDEX authorization_scope_index on 
--	oauth2_authorization_scope(authorization_id, scope_id);

CREATE TABLE IF NOT EXISTS oauth2_refresh_token (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	token VARCHAR(255) NOT NULL,
	user_id VARCHAR(100) DEFAULT NULL,
	user_auth_time TIMESTAMP NOT NULL,
	created_date TIMESTAMP NOT NULL,
	expiry_date TIMESTAMP NULL,
	is_revoked BOOLEAN DEFAULT 0,
	client VARCHAR(100) NOT NULL,
	FOREIGN KEY (client)
	   REFERENCES oauth2_client(id)
	   ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth2_refresh_token_scope (
	token_id INTEGER NOT NULL, 
	scope_id VARCHAR(100) NOT NULL, 
	CONSTRAINT primary_key PRIMARY KEY (token_id, scope_id)
);

CREATE TABLE IF NOT EXISTS oauth2_access_token (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	token VARCHAR(255) NOT NULL,
	user_id VARCHAR(100) DEFAULT NULL,
	created_date TIMESTAMP NOT NULL,
	expiry_date TIMESTAMP NOT NULL,
	is_revoked BOOLEAN DEFAULT 0,
	user_auth_time TIMESTAMP NOT NULL,
	refresh_token INTEGER DEFAULT NULL,
	client VARCHAR(100) DEFAULT NULL,
	FOREIGN KEY (client)
	   REFERENCES oauth2_client(id)
	   ON DELETE CASCADE
	FOREIGN KEY (refresh_token)
	   REFERENCES oauth2_refresh_token(id)
);

CREATE TABLE IF NOT EXISTS oauth2_access_token_scope (
	token_id INTEGER NOT NULL, 
	scope_id VARCHAR(100) NOT NULL, 
	CONSTRAINT primary_key PRIMARY KEY (token_id, scope_id)
);

