-- roles
INSERT INTO role(name) VALUES ("group admin");
INSERT INTO role(name) VALUES ("group member");
INSERT INTO role(name) VALUES ("VC admin");
INSERT INTO role(name) VALUES ("VC member");

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