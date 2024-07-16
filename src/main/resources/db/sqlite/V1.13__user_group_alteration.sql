--DROP INDEX IF EXISTS group_member_role_index;
--DROP INDEX IF EXISTS user_group_member_index;
DROP INDEX IF EXISTS user_group_member_status_index;
DROP INDEX IF EXISTS role_index;

-- please commented out the triggers in V1.2__triggers.sql later
--DROP TRIGGER IF EXISTS insert_member_status;
--DROP TRIGGER IF EXISTS update_member_status;
--DROP TRIGGER IF EXISTS delete_member;

--ALTER TABLE user_group
--DROP COLUMN deleted_by;
--
ALTER TABLE user_group
ADD COLUMN created_date TIMESTAMP;
--
--ALTER TABLE user_group_member
--DROP COLUMN created_by;
--
--ALTER TABLE user_group_member
--DROP COLUMN deleted_by;
--
--ALTER TABLE user_group_member
--DROP COLUMN status;
--
--ALTER TABLE user_group_member
--DROP COLUMN status_date;


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
  SELECT DISTINCT r.name || '_' || p.name, p.name, ug.id, qa.query_id
  FROM user_group ug 
  JOIN query_access qa ON ug.id=qa.user_group_id
  JOIN user_group_member ugm ON ugm.group_id = ug.id
  JOIN group_member_role gmr ON gmr.group_member_id = ugm.id
  JOIN role r ON gmr.role_id = r.id
  JOIN privilege p ON p.role_id = r.id;

--CREATE TABLE IF NOT EXISTS user_role (
--  id INTEGER PRIMARY KEY AUTOINCREMENT,
--  user_id VARCHAR(100) NOT NULL,
--  status VARCHAR(100) NOT NULL,
--  role_id INTEGER,
--  FOREIGN KEY (role_id) 
--  	REFERENCES role_new (id)
--  	ON DELETE CASCADE
--);
--
--INSERT INTO user_role (user_id,status,role_id)
--SELECT  ugm.user_id, ugm.status, rn.id
--  FROM query_access qa 
--  JOIN user_group_member ugm ON ugm.group_id = qa.user_group_id  
--  JOIN group_member_role gmr ON gmr.group_member_id = ugm.id
--  JOIN role r ON gmr.role_id = r.id
--  JOIN privilege p ON p.role_id = r.id
--  JOIN role_new rn
--  where r.name || '_' || p.name = rn.name
--  and rn.group_id=qa.user_group_id;
  
DROP INDEX IF EXISTS privilege_index;
DROP INDEX IF EXISTS virtual_corpus_access_unique_index;
DROP INDEX IF EXISTS virtual_corpus_status_index;

DROP TABLE role;

ALTER TABLE role_new RENAME TO role;

DROP TABLE privilege;
--DROP TABLE group_member_role;
--DROP TABLE query_access;
--DROP TABLE user_group_member;