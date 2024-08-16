DROP INDEX IF EXISTS role_index;

CREATE TABLE IF NOT EXISTS role_new (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name VARCHAR(100) NOT NULL,
  privilege VARCHAR(100) NOT NULL,
  group_id INTEGER,
  query_id INTEGER,
  FOREIGN KEY (group_id) 
  	REFERENCES user_group (id)
  	ON DELETE CASCADE
  FOREIGN KEY (query_id) 
  	REFERENCES query (id)
  	ON DELETE CASCADE	
);

INSERT INTO role_new (name, privilege, group_id, query_id)
  SELECT DISTINCT r.name, p.name, ug.id, qa.query_id
  FROM user_group ug 
  JOIN query_access qa ON ug.id=qa.user_group_id
  JOIN user_group_member ugm ON ugm.group_id = ug.id
  JOIN group_member_role gmr ON gmr.group_member_id = ugm.id
  JOIN role r ON gmr.role_id = r.id
  JOIN privilege p ON p.role_id = r.id;

DROP INDEX IF EXISTS privilege_index;
DROP INDEX IF EXISTS virtual_corpus_access_unique_index;
DROP INDEX IF EXISTS virtual_corpus_status_index;

DROP TABLE role;

ALTER TABLE role_new RENAME TO role;

DROP TABLE privilege;
DROP TABLE query_access;

CREATE UNIQUE INDEX IF NOT EXISTS role_index_null_query
ON role (name, privilege, group_id)
WHERE query_id IS 0;

CREATE UNIQUE INDEX IF NOT EXISTS role_index on role(name, 
  privilege, group_id, query_id);
  