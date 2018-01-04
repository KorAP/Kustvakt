-- dummy data only for testing

-- user groups
INSERT INTO user_group(name,status,created_by) 
	VALUES ("dory group","ACTIVE","dory");

INSERT INTO user_group(name,status,created_by) 
	VALUES ("auto group","HIDDEN","system");

--INSERT INTO user_group(name,status,created_by) 
--	VALUES ("all users","HIDDEN","system");

INSERT INTO user_group(name,status,created_by) 
	VALUES ("deleted group","DELETED","dory");



-- user group members
INSERT INTO user_group_member(user_id, group_id, status, created_by)
	SELECT "dory",
		(SELECT id from user_group where name = "dory group"),
		"ACTIVE","dory";

INSERT INTO user_group_member(user_id, group_id, status, created_by)
	SELECT "nemo",
		(SELECT id from user_group where name = "dory group"),
		"ACTIVE","dory";

INSERT INTO user_group_member(user_id, group_id, status, created_by)
	SELECT "marlin",
		(SELECT id from user_group where name = "dory group"),
		"PENDING","dory";
	
INSERT INTO user_group_member(user_id, group_id, status, created_by, deleted_by)
	SELECT "pearl",
		(SELECT id from user_group where name = "dory group"),
		"DELETED","dory", "pearl";

INSERT INTO user_group_member(user_id, group_id, status, created_by)
	SELECT "pearl",
		(SELECT id from user_group where name = "auto group"),
		"ACTIVE","system";

		
-- virtual corpora
INSERT INTO virtual_corpus(name, type, required_access, created_by, description, status, collection_query) 
	VALUES ("dory VC", "PRIVATE", "FREE", "dory", "test vc", "experimental",
	'{"collection": { "@type": "koral:docGroup", "operands": [ { "@type": "koral:doc", "key": "corpusSigle", "match": "match:eq", "value": "GOE" }, { "@type": "koral:doc", "key": "creationDate", "match": "match:geq", "type": "type:date", "value": "1820" } ], "operation": "operation:and" }}');
	
INSERT INTO virtual_corpus(name, type, required_access, created_by, description, status, collection_query) 
	VALUES ("group VC", "PROJECT", "PUB", "dory", "test vc", "experimental",
	'{"collection": { "@type": "koral:docGroup", "operands": [ { "@type": "koral:doc", "key": "corpusSigle", "match": "match:eq", "value": "GOE" }, { "@type": "koral:doc", "key": "creationDate", "match": "match:leq", "type": "type:date", "value": "1810" } ], "operation": "operation:and" }}');

INSERT INTO virtual_corpus(name, type, required_access, created_by, description, status, collection_query) 
	VALUES ("system VC", "PREDEFINED", "ALL", "system", "test vc", "experimental",
	'{"collection":{"@type":"koral:doc","value":"GOE","match":"match:eq","key":"corpusSigle"}}');

INSERT INTO virtual_corpus(name, type, required_access, created_by, description, status, collection_query) 
	VALUES ("published VC", "PUBLISHED", "ALL", "marlin", "test vc", "experimental",
	'{"collection":{"@type":"koral:doc","value":"GOE","match":"match:eq","key":"corpusSigle"}}');


-- virtual corpus access
INSERT INTO virtual_corpus_access(virtual_corpus_id, user_group_id, status, created_by) 
	SELECT 
		(SELECT id from virtual_corpus where name = "group VC"), 
		(SELECT id from user_group where name = "dory group"), 
		"ACTIVE", "dory";

INSERT INTO virtual_corpus_access(virtual_corpus_id, user_group_id, status, created_by) 
	SELECT 
		(SELECT id from virtual_corpus where name = "system VC"), 
		(SELECT id from user_group where name = "all users"),
		"ACTIVE", "system";

INSERT INTO virtual_corpus_access(virtual_corpus_id, user_group_id, status, created_by) 
	SELECT 
		(SELECT id from virtual_corpus where name = "published VC"),
		(SELECT id from user_group where name = "all users"),
		"HIDDEN", "system";

INSERT INTO virtual_corpus_access(virtual_corpus_id, user_group_id, status, created_by) 
	SELECT 
		(SELECT id from virtual_corpus where name = "published VC"),
		(SELECT id from user_group where name = "auto group"),
		"ACTIVE", "system";

	
-- Summary user VC Lists
-- dory: dory VC, group VC, system VC
-- nemo: group VC, system VC
-- marlin: published VC, system VC
-- pearl: system VC, published VC
