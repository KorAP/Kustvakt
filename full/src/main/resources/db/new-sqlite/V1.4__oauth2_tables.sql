-- EM: modified from Michael Hanl version

-- oauth2 db tables
CREATE TABLE IF NOT EXISTS oauth2_client (
	id VARCHAR(100) PRIMARY KEY NOT NULL,
	name VARCHAR(255) NOT NULL,
	secret VARCHAR(255) DEFAULT NULL,
	type VARCHAR(255) NOT NULL,
	native BOOLEAN DEFAULT FALSE,
	url TEXT DEFAULT NULL,
	url_hashcode INTEGER,
	redirect_uri TEXT DEFAULT NULL,
	description VARCHAR(255) NOT NULL,
	registered_by VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX client_id_index on oauth2_client(id);
CREATE UNIQUE INDEX client_url_index on oauth2_client(url_hashcode);

CREATE TABLE IF NOT EXISTS oauth2_authorization (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	code VARCHAR(255) NOT NULL,
	client_id VARCHAR(100) NOT NULL,
	user_id VARCHAR(100) NOT NULL,
	redirect_uri TEXT DEFAULT NULL,
	created_date TIMESTAMP DEFAULT (datetime('now','localtime')),
	is_revoked BOOLEAN DEFAULT 0,
	total_attempts INTEGER DEFAULT 0,
	user_auth_time TIMESTAMP NOT NULL,
	nonce TEXT DEFAULT NULL,
	FOREIGN KEY (client_id)
	   REFERENCES oauth2_client(id)
);

CREATE UNIQUE INDEX authorization_index on oauth2_authorization(code, client_id);

CREATE TABLE IF NOT EXISTS oauth2_access_scope (
	id VARCHAR(255) PRIMARY KEY NOT NULL
);

CREATE TABLE IF NOT EXISTS oauth2_authorization_scope (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	authorization_id INTEGER NOT NULL,
	scope_id VARCHAR(100) NOT NULL,
	FOREIGN KEY (authorization_id)
	   REFERENCES oauth2_authorization(id),
	FOREIGN KEY (scope_id)
	   REFERENCES oauth2_access_scope(id)
);

CREATE UNIQUE INDEX authorization_scope_index on 
	oauth2_authorization_scope(authorization_id, scope_id);

CREATE TABLE IF NOT EXISTS oauth2_access_token (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	token VARCHAR(255) NOT NULL,
	authorization_id INTEGER DEFAULT NULL,
	user_id VARCHAR(100) DEFAULT NULL,
	created_date TIMESTAMP DEFAULT (datetime('now','localtime')),
	is_revoked BOOLEAN DEFAULT 0,
	total_attempts INTEGER DEFAULT 0,
	user_auth_time TIMESTAMP NOT NULL,
	FOREIGN KEY (authorization_id)
	   REFERENCES oauth2_authorization(id)
);

CREATE TABLE oauth2_access_token_scope (
	token_id INTEGER NOT NULL, 
	scope_id VARCHAR(100) NOT NULL, 
	primary key (token_id, scope_id)
);
