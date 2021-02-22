-- roles
INSERT INTO role(name) VALUES ("USER_GROUP_ADMIN");
INSERT INTO role(name) VALUES ("USER_GROUP_MEMBER");
INSERT INTO role(name) VALUES ("QUERY_ACCESS_ADMIN");
INSERT INTO role(name) VALUES ("QUERY_ACCESS_MEMBER");

-- privileges
INSERT INTO privilege(name,role_id)
	VALUES("READ", 1);
INSERT INTO privilege(name,role_id)
	VALUES("WRITE", 1);
INSERT INTO privilege(name,role_id)
	VALUES("DELETE", 1);
	
INSERT INTO privilege(name,role_id)
	VALUES("DELETE",2);
	
INSERT INTO privilege(name,role_id)
	VALUES("READ",3);
INSERT INTO privilege(name,role_id)
	VALUES("WRITE",3);
INSERT INTO privilege(name,role_id)
	VALUES("DELETE",3);

INSERT INTO privilege(name,role_id)
	VALUES("READ",4);	