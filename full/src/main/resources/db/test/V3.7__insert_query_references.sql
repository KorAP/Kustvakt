-- query references
INSERT INTO query(name, type, query_type, required_access, created_by, description, status, 
    koral_query, query, query_language) 
	VALUES ("dory-q", "PRIVATE", "QUERY", "FREE", "dory", "test query", "experimental",
	'{ "@type": "koral:token" }', "[]", "poliqarp");

INSERT INTO query(name, type, query_type, required_access, created_by, description, status, 
    koral_query, query, query_language) 
	VALUES ("system-q", "SYSTEM", "QUERY", "FREE", "system", '"system" query', "experimental",
	'{ "@type": "koral:token" }', "[]", "poliqarp");
