-- EM: modified from Michael Hanl version

-- oauth2 db tables
CREATE TABLE IF NOT EXISTS oauth2_client (
	id VARCHAR(100) PRIMARY KEY NOT NULL,
	name VARCHAR(200) NOT NULL,
	secret VARCHAR(200) DEFAULT NULL,
	type VARCHAR(200) NOT NULL,
	super BOOLEAN DEFAULT FALSE,
	redirect_uri TEXT DEFAULT NULL,
	description VARCHAR(250) NOT NULL,
	registered_by VARCHAR(100) NOT NULL,
	url_hashcode INTEGER NOT NULL,	
	url TEXT DEFAULT NULL,
	UNIQUE INDEX unique_url(url_hashcode)
);

CREATE TABLE IF NOT EXISTS oauth2_access_scope (
	id VARCHAR(255) PRIMARY KEY NOT NULL
);

-- authorization tables are not needed if using cache 

--CREATE TABLE IF NOT EXISTS oauth2_authorization (
--	id INTEGER PRIMARY KEY AUTO_INCREMENT,
--	code VARCHAR(255) NOT NULL,
--	client_id VARCHAR(100) NOT NULL,
--	user_id VARCHAR(100) NOT NULL,
--	redirect_uri TEXT DEFAULT NULL,
--	created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--	expiry_date TIMESTAMP NULL,
--	is_revoked BOOLEAN DEFAULT 0,
--	total_attempts INTEGER DEFAULT 0,
--	user_auth_time TIMESTAMP NULL,
--	nonce TEXT DEFAULT NULL,
--	FOREIGN KEY (client_id)
--	   REFERENCES oauth2_client(id),
--	UNIQUE INDEX authorization_index(code, client_id)
--);
--
--CREATE TABLE IF NOT EXISTS oauth2_authorization_scope (
--	id INTEGER PRIMARY KEY AUTO_INCREMENT,
--	authorization_id INTEGER NOT NULL,
--	scope_id VARCHAR(100) NOT NULL,
--	FOREIGN KEY (authorization_id)
--	   REFERENCES oauth2_authorization(id),
--	FOREIGN KEY (scope_id)
--	   REFERENCES oauth2_access_scope(id),
--	UNIQUE INDEX authorization_scope_index(authorization_id, scope_id)
--);

CREATE TABLE IF NOT EXISTS oauth2_refresh_token (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	token VARCHAR(255) NOT NULL,
	user_id VARCHAR(100) DEFAULT NULL,
	user_auth_time TIMESTAMP NOT NULL,
	created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	expiry_date TIMESTAMP NULL,
	is_revoked BOOLEAN DEFAULT 0,
	client VARCHAR(100) NOT NULL,
	FOREIGN KEY (client)
	   REFERENCES oauth2_client(id)
	   -- these will delete all refresh tokens related to the client
	   ON DELETE CASCADE
);

CREATE TABLE oauth2_refresh_token_scope (
	token_id INTEGER NOT NULL, 
	scope_id VARCHAR(100) NOT NULL, 
	CONSTRAINT primary_key PRIMARY KEY (token_id, scope_id)
);

CREATE TABLE IF NOT EXISTS oauth2_access_token (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	token VARCHAR(255) NOT NULL,
	user_id VARCHAR(100) DEFAULT NULL,
	client_id VARCHAR(100) DEFAULT NULL,
	created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	expiry_date TIMESTAMP NULL,
	is_revoked BOOLEAN DEFAULT 0,
	user_auth_time TIMESTAMP NULL,
    refresh_token INTEGER DEFAULT NULL,
	FOREIGN KEY (client_id)
	   REFERENCES oauth2_client(id)
	   -- these will delete all access tokens related to the client
	   ON DELETE CASCADE,
	FOREIGN KEY (refresh_token)
	   REFERENCES oauth2_refresh_token(id)
);

CREATE TABLE oauth2_access_token_scope (
	token_id INTEGER NOT NULL, 
	scope_id VARCHAR(100) NOT NULL, 
	CONSTRAINT primary_key PRIMARY KEY (token_id, scope_id)
);

