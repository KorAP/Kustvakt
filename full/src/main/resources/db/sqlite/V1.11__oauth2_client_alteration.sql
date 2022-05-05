CREATE TABLE IF NOT EXISTS oauth2_client_new (
	id VARCHAR(100) PRIMARY KEY NOT NULL,
	name VARCHAR(100) NOT NULL,
	secret VARCHAR(255) DEFAULT NULL,
	type VARCHAR(50) NOT NULL,
	super BOOLEAN DEFAULT FALSE,
	redirect_uri TEXT DEFAULT NULL,
	description VARCHAR(255) NOT NULL,
	registered_by VARCHAR(100) NOT NULL,
	--url_hashcode INTEGER,	
	url TEXT DEFAULT NULL,
	registration_date TIMESTAMP,
	refresh_token_expiry INTEGER DEFAULT 31536000,
	source BLOB DEFAULT NULL,
	is_permitted BOOLEAN DEFAULT FALSE
);

INSERT INTO oauth2_client_new(id,name,secret,type,super,redirect_uri,description,registered_by,url) 
	SELECT id,name,secret,type,super,redirect_uri,description,registered_by,url FROM oauth2_client;

DROP TABLE oauth2_client;

ALTER TABLE oauth2_client_new RENAME TO oauth2_client;
