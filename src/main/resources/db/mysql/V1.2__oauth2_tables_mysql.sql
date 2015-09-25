
-- oauth2 db tables
create table oauth2_client (
client_id VARCHAR(100) UNIQUE PRIMARY KEY,
client_secret VARCHAR(200),
redirect_uri VARCHAR(250),
client_type VARCHAR(200),
is_confidential BOOLEAN DEFAULT FALSE,
url VARCHAR(200) UNIQUE
);


-- status 1 = valid, 0 = revoked, -1 = disabled
create table oauth2_access_token (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
access_token VARCHAR(300),
auth_code VARCHAR(250),
client_id VARCHAR(100),
user_id INTEGER,
-- make boolean --
status INTEGER DEFAULT 1,
-- in case of code authorization, should match auth code scopes!
-- use scopes for levelaccess descriptor level[rw],level[r]
scopes VARCHAR(350),
expiration TIMESTAMP,
FOREIGN KEY (user_id)
REFERENCES korap_users(id)
ON DELETE CASCADE,
FOREIGN KEY (client_id)
REFERENCES oauth2_client(client_id)
ON DELETE CASCADE
);


-- also scopes?
create table oauth2_refresh_token (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
client_id VARCHAR(100),
user_id INTEGER,
expiration TIMESTAMP,
scopes VARCHAR(350),
FOREIGN KEY (user_id)
REFERENCES korap_users(id)
ON DELETE CASCADE,
FOREIGN KEY (client_id)
REFERENCES oauth2_client(client_id)
ON DELETE CASCADE
);