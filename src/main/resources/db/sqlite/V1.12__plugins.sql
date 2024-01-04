CREATE TABLE IF NOT EXISTS installed_plugin (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	installed_by VARCHAR(100) NOT NULL,
	installed_date TIMESTAMP NOT NULL,
	client_id VARCHAR(100) NOT NULL,
	super_client_id VARCHAR(100) NOT NULL,
	FOREIGN KEY (client_id)
	   REFERENCES oauth2_client(id)
	   ON DELETE CASCADE
	FOREIGN KEY (super_client_id)
	   REFERENCES oauth2_client(id)
	   ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS unique_installed_plugin 
	on installed_plugin(installed_by,client_id,super_client_id);
