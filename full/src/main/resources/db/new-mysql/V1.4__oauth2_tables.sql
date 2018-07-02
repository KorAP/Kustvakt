-- EM: modified from Michael Hanl version

-- oauth2 db tables
CREATE TABLE IF NOT EXISTS oauth2_client (
	id VARCHAR(100) PRIMARY KEY NOT NULL,
	name VARCHAR(200) NOT NULL,
	secret VARCHAR(200) DEFAULT NULL,
	type VARCHAR(200) NOT NULL,
	native BOOLEAN DEFAULT FALSE,
	url TEXT DEFAULT NULL,
	url_hashcode INTEGER,
	redirect_uri TEXT DEFAULT NULL,
	description VARCHAR(250) NOT NULL,
	registered_by VARCHAR(100) NOT NULL,
	UNIQUE INDEX unique_url(url_hashcode)
);

CREATE TABLE IF NOT EXISTS oauth2_authorization (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	code VARCHAR(255) NOT NULL,
	client_id VARCHAR(100) NOT NULL,
	user_id VARCHAR(100) NOT NULL,
	redirect_uri TEXT DEFAULT NULL,
	created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	is_revoked BOOLEAN DEFAULT 0,
	total_attempts INTEGER DEFAULT 0,
	user_auth_time TIMESTAMP NOT NULL,
	nonce TEXT DEFAULT NULL,
	FOREIGN KEY (client_id)
	   REFERENCES oauth2_client(id),
	UNIQUE INDEX authorization_index(code, client_id)
);

CREATE TABLE IF NOT EXISTS oauth2_access_scope (
	id VARCHAR(255) PRIMARY KEY NOT NULL
);

CREATE TABLE IF NOT EXISTS oauth2_authorization_scope (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	authorization_id INTEGER NOT NULL,
	scope_id VARCHAR(100) NOT NULL,
	FOREIGN KEY (authorization_id)
	   REFERENCES oauth2_authorization(id),
	FOREIGN KEY (scope_id)
	   REFERENCES oauth2_access_scope(id),
	UNIQUE INDEX authorization_scope_index(authorization_id, scope_id)
);

CREATE TABLE IF NOT EXISTS oauth2_access_token (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	token VARCHAR(255) NOT NULL,
	authorization_id INTEGER DEFAULT NULL,
	user_id VARCHAR(100) DEFAULT NULL,
	created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	is_revoked BOOLEAN DEFAULT 0,
	total_attempts INTEGER DEFAULT 0,
	user_auth_time TIMESTAMP NOT NULL,
	FOREIGN KEY (authorization_id)
	   REFERENCES oauth2_authorization(id)
);

CREATE TABLE oauth2_access_token_scope (
	token_id INTEGER NOT NULL, 
	scope_id VARCHAR(100) NOT NULL, 
	CONSTRAINT primary_key PRIMARY KEY (token_id, scope_id)
);

--
---- status 1 = valid, 0 = revoked, -1 = disabled
--create table if not exists oauth2_access_token (
--id INTEGER PRIMARY KEY AUTO_INCREMENT,
--access_token VARCHAR(300),
--auth_code VARCHAR(250),
--client_id VARCHAR(100),
--user_id INTEGER,
---- make boolean --
--status INTEGER DEFAULT 1,
---- in case of code authorization, should match auth code scopes!
---- use scopes for levelaccess descriptor level[rw],level[r]
--scopes VARCHAR(350),
--expiration TIMESTAMP,
--FOREIGN KEY (user_id)
--REFERENCES korap_users(id)
--ON DELETE CASCADE,
--FOREIGN KEY (client_id)
--REFERENCES oauth2_client(client_id)
--ON DELETE CASCADE
--);
--
--
---- also scopes?
--create table if not exists oauth2_refresh_token (
--id INTEGER PRIMARY KEY AUTO_INCREMENT,
--client_id VARCHAR(100),
--user_id INTEGER,
--expiration TIMESTAMP,
--scopes VARCHAR(350),
--FOREIGN KEY (user_id)
--REFERENCES korap_users(id)
--ON DELETE CASCADE,
--FOREIGN KEY (client_id)
--REFERENCES oauth2_client(client_id)
--ON DELETE CASCADE
--);