-- EM: modified from Michael Hanl version

-- oauth2 db tables
create table IF NOT EXISTS oauth2_client (
	id VARCHAR(100) NOT NULL,
	name VARCHAR(200) NOT NULL,
	secret VARCHAR(200) NOT NULL,
	type VARCHAR(200) NOT NULL,
	url TEXT UNIQUE NOT NULL,
	redirect_uri TEXT NOT NULL,
	registeredBy VARCHAR(100) NOT NULL
);
