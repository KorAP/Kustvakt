-- EM: modified from Michael Hanl version

-- oauth2 db tables
CREATE TABLE IF NOT EXISTS oauth2_client (
	id VARCHAR(100) PRIMARY KEY NOT NULL,
	name VARCHAR(200) NOT NULL,
	secret VARCHAR(200),
	type VARCHAR(200) NOT NULL,
	native BOOLEAN DEFAULT FALSE,
	url TEXT NOT NULL,
	url_hashcode INTEGER NOT NULL,
	redirect_uri TEXT NOT NULL,
	registered_by VARCHAR(100) NOT NULL,
	description VARCHAR(250) NOT NULL
);

CREATE UNIQUE INDEX client_id_index on oauth2_client(id);
CREATE UNIQUE INDEX client_url_index on oauth2_client(url_hashcode);

CREATE TABLE IF NOT EXISTS oauth2_authorization (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	code VARCHAR(250) NOT NULL,
	client_id VARCHAR(100) NOT NULL,
	user_id VARCHAR(100) NOT NULL,
	redirect_uri TEXT DEFAULT NULL,
	created_date timestamp DEFAULT (datetime('now','localtime')),
	is_revoked BOOLEAN DEFAULT 0,
	total_attempts INTEGER DEFAULT 0,
	FOREIGN KEY (client_id)
	   REFERENCES oauth2_client(id)
);

CREATE UNIQUE INDEX authorization_index on oauth2_authorization(code, client_id);

CREATE TABLE IF NOT EXISTS oauth2_access_scope (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	name VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS oauth2_authorization_scope (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	authorization_id INTEGER NOT NULL,
	scope_id INTEGER NOT NULL,
	FOREIGN KEY (authorization_id)
	   REFERENCES oauth2_authorization(id),
	FOREIGN KEY (scope_id)
	   REFERENCES access_scope(id)
);

CREATE UNIQUE INDEX authorization_scope_index on 
	oauth2_authorization_scope(authorization_id, scope_id);

CREATE TRIGGER insert_created_date AFTER INSERT ON oauth2_authorization
     BEGIN
      UPDATE oauth2_authorization
      SET created_date = DATETIME('now', 'localtime')  
      WHERE rowid = new.rowid;
     END;
     
CREATE TABLE IF NOT EXISTS oauth2_access_token (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	token VARCHAR(300) NOT NULL,
	authorization_id INTEGER DEFAULT NULL,
	created_date timestamp DEFAULT (datetime('now','localtime')),
	is_revoked BOOLEAN DEFAULT 0,
	total_attempts INTEGER DEFAULT 0,
	FOREIGN KEY (authorization_id)
	   REFERENCES oauth2_authorization(id)
);

