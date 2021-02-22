-- dummy data only for testing

-- user groups
INSERT INTO user_group(name,status,created_by) 
	VALUES ("marlin-group","ACTIVE","marlin");
	
INSERT INTO user_group(name,status,created_by) 
	VALUES ("dory-group","ACTIVE","dory");

INSERT INTO user_group(name,status,created_by) 
	VALUES ("auto-group","HIDDEN","system");

--INSERT INTO user_group(name,status,created_by) 
--	VALUES ("all users","HIDDEN","system");

INSERT INTO user_group(name,status,created_by, deleted_by) 
	VALUES ("deleted-group","DELETED","dory", "dory");



-- user group members
INSERT INTO user_group_member(user_id, group_id, status, created_by)
	SELECT "marlin",
		(SELECT id from user_group where name = "marlin-group"),
		"ACTIVE","marlin";

INSERT INTO user_group_member(user_id, group_id, status, created_by)
	SELECT "dory",
		(SELECT id from user_group where name = "marlin-group"),
		"ACTIVE","marlin";
		
INSERT INTO user_group_member(user_id, group_id, status, created_by)
	SELECT "dory",
		(SELECT id from user_group where name = "dory-group"),
		"ACTIVE","dory";

INSERT INTO user_group_member(user_id, group_id, status, created_by)
	SELECT "nemo",
		(SELECT id from user_group where name = "dory-group"),
		"ACTIVE","dory";

INSERT INTO user_group_member(user_id, group_id, status, created_by)
	SELECT "marlin",
		(SELECT id from user_group where name = "dory-group"),
		"PENDING","dory";
	
INSERT INTO user_group_member(user_id, group_id, status, created_by, deleted_by)
	SELECT "pearl",
		(SELECT id from user_group where name = "dory-group"),
		"DELETED","dory", "pearl";

INSERT INTO user_group_member(user_id, group_id, status, created_by)
	SELECT "pearl",
		(SELECT id from user_group where name = "auto-group"),
		"ACTIVE","system";

INSERT INTO user_group_member(user_id, group_id, status, created_by)
	SELECT "dory",
		(SELECT id from user_group where name = "deleted-group"),
		"ACTIVE","dory";

		
-- virtual corpora
INSERT INTO query(name, type, query_type, required_access, created_by, description, status, koral_query) 
	VALUES ("dory-vc", "PRIVATE", "VIRTUAL_CORPUS", "FREE", "dory", "test vc", "experimental",
	'{"collection": { "@type": "koral:docGroup", "operands": [ { "@type": "koral:doc", "key": "corpusSigle", "match": "match:eq", "value": "GOE" }, { "@type": "koral:doc", "key": "creationDate", "match": "match:geq", "type": "type:date", "value": "1820" } ], "operation": "operation:and" }}');
	
INSERT INTO query(name, type, query_type, required_access, created_by, description, status, koral_query) 
	VALUES ("group-vc", "PROJECT", "VIRTUAL_CORPUS", "PUB", "dory", "test vc", "experimental",
	'{"collection": { "@type": "koral:docGroup", "operands": [ { "@type": "koral:doc", "key": "corpusSigle", "match": "match:eq", "value": "GOE" }, { "@type": "koral:doc", "key": "creationDate", "match": "match:leq", "type": "type:date", "value": "1810" } ], "operation": "operation:and" }}');

INSERT INTO query(name, type, query_type, required_access, created_by, description, status, koral_query) 
	VALUES ("system-vc", "SYSTEM", "VIRTUAL_CORPUS", "ALL", "system", "test vc", "experimental",
	'{"collection":{"@type":"koral:doc","value":"GOE","match":"match:eq","key":"corpusSigle"}}');

INSERT INTO query(name, type, query_type, required_access, created_by, description, status, koral_query) 
	VALUES ("published-vc", "PUBLISHED", "VIRTUAL_CORPUS", "ALL", "marlin", "test vc", "experimental",
	'{"collection":{"@type":"koral:doc","value":"GOE","match":"match:eq","key":"corpusSigle"}}');

INSERT INTO query(name, type, query_type, required_access, created_by, description, status, koral_query) 
	VALUES ("marlin-vc", "PRIVATE", "VIRTUAL_CORPUS", "FREE", "marlin", "marlin test share vc", "experimental",
	'{"collection": { "@type": "koral:docGroup", "operands": [ { "@type": "koral:doc", "key": "corpusSigle", "match": "match:eq", "value": "GOE" }, { "@type": "koral:doc", "key": "creationDate", "match": "match:geq", "type": "type:date", "value": "1820" } ], "operation": "operation:and" }}');

INSERT INTO query(name, type, query_type, required_access, created_by, description, status, koral_query) 
	VALUES ("nemo-vc", "PRIVATE", "VIRTUAL_CORPUS", "ALL", "nemo", "nemo test vc", "experimental",
	'{"collection":{"@type":"koral:doc","value":"GOE","match":"match:eq","key":"corpusSigle"}}');	
	
-- virtual corpus access
INSERT INTO query_access(query_id, user_group_id, status, created_by) 
	SELECT 
		(SELECT id from query where name = "group-vc"), 
		(SELECT id from user_group where name = "dory-group"), 
		"ACTIVE", "dory";

--INSERT INTO query_access(query_id, user_group_id, status, created_by) 
--	SELECT 
--		(SELECT id from query where name = "system-vc"), 
--		(SELECT id from user_group where name = "all users"),
--		"ACTIVE", "system";

INSERT INTO query_access(query_id, user_group_id, status, created_by) 
	SELECT 
		(SELECT id from query where name = "published-vc"),
		(SELECT id from user_group where name = "marlin-group"),
		"ACTIVE", "marlin";

INSERT INTO query_access(query_id, user_group_id, status, created_by) 
	SELECT 
		(SELECT id from query where name = "published-vc"),
		(SELECT id from user_group where name = "auto-group"),
		"HIDDEN", "system";

	
-- Summary user VC Lists
-- dory: dory-vc, group-vc, system-vc
-- nemo: group-vc, system-vc
-- marlin: published-vc, system-vc
-- pearl: system-vc, published-vc
