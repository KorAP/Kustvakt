-- EM: modified from Michael Hanl version

-- oauth2 db tables
CREATE TABLE IF NOT EXISTS oauth2_client (
	id VARCHAR(100) PRIMARY KEY NOT NULL,
	name VARCHAR(200) NOT NULL,
	secret VARCHAR(200),
	type VARCHAR(200) NOT NULL,
	url TEXT NOT NULL,
	url_hashcode INTEGER NOT NULL,
	redirect_uri TEXT NOT NULL,
	registeredBy VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX client_id_index on oauth2_client(id);
CREATE UNIQUE INDEX client_url_index on oauth2_client(url_hashcode);
