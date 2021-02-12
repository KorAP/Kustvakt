-- query references
INSERT INTO virtual_corpus(name, type, query_type, required_access, created_by, description, status, corpus_query) 
	VALUES ("dory-q", "PRIVATE", "QUERY", "FREE", "dory", "test query", "experimental",
	'{ "@type": "koral:token" }');

INSERT INTO virtual_corpus(name, type, query_type, required_access, created_by, description, status, corpus_query) 
	VALUES ("system-q", "SYSTEM", "QUERY", "FREE", "system", "system query", "experimental",
	'{ "@type": "koral:token" }');
