ALTER TABLE oauth2_client 
	ADD COLUMN registration_date TIMESTAMP;

-- default 365 days in seconds
ALTER TABLE oauth2_client 
	ADD COLUMN refresh_token_expiry INTEGER DEFAULT 31536000;
	
ALTER TABLE oauth2_client 
	ADD COLUMN source BLOB DEFAULT NULL;

ALTER TABLE oauth2_client 
	ADD COLUMN is_permitted BOOLEAN DEFAULT FALSE;

--CREATE TABLE IF NOT EXISTS user_installed_client (
--	id INTEGER PRIMARY KEY AUTOINCREMENT,
--	installed_by VARCHAR(100) NOT NULL,
--	installed_date TIMESTAMP NOT NULL,
--	client_id VARCHAR(100) NOT NULL,
--	FOREIGN KEY (client_id)
--	   REFERENCES oauth2_client(id)
--	   ON DELETE CASCADE
--);