CREATE TABLE IF NOT EXISTS oauth2_refresh_token_scope_new (
	token_id INTEGER NOT NULL, 
	scope_id VARCHAR(100) NOT NULL, 
	FOREIGN KEY (token_id)
	   REFERENCES oauth2_refresh_token(id)
	   ON DELETE CASCADE
	FOREIGN KEY (scope_id)
	   REFERENCES oauth2_access_scope(id)
	   ON DELETE CASCADE
	CONSTRAINT primary_key PRIMARY KEY (token_id, scope_id)
);

INSERT INTO oauth2_refresh_token_scope_new SELECT * FROM oauth2_refresh_token_scope;

DROP TABLE oauth2_refresh_token_scope;

ALTER TABLE oauth2_refresh_token_scope_new RENAME TO oauth2_refresh_token_scope;


CREATE TABLE IF NOT EXISTS oauth2_access_token_scope_new (
	token_id INTEGER NOT NULL, 
	scope_id VARCHAR(100) NOT NULL, 
	FOREIGN KEY (token_id)
	   REFERENCES oauth2_access_token(id)
	   ON DELETE CASCADE
	FOREIGN KEY (scope_id)
	   REFERENCES oauth2_access_scope(id)
	   ON DELETE CASCADE
	CONSTRAINT primary_key PRIMARY KEY (token_id, scope_id)
);

INSERT INTO oauth2_access_token_scope_new SELECT * FROM oauth2_access_token_scope;

DROP TABLE oauth2_access_token_scope;

ALTER TABLE oauth2_access_token_scope_new RENAME TO oauth2_access_token_scope;
