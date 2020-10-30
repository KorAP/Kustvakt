-- query references
INSERT INTO query_reference(name, type, required_access, created_by, description, status, query) 
	VALUES ("dory-q", "PRIVATE", "FREE", "dory", "test query", "experimental",
	'{ "@type": "koral:token" }');
