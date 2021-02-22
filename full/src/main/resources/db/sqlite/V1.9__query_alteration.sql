ALTER TABLE virtual_corpus 
ADD COLUMN query_type VARCHAR(100) NOT NULL;

ALTER TABLE virtual_corpus 
ADD COLUMN query TEXT DEFAULT NULL;

ALTER TABLE virtual_corpus 
ADD COLUMN query_language VARCHAR(100) DEFAULT NULL;

ALTER TABLE virtual_corpus 
RENAME COLUMN corpus_query TO koral_query;

ALTER TABLE virtual_corpus
RENAME TO query;

DROP INDEX IF EXISTS virtual_corpus_owner_index;
DROP INDEX IF EXISTS virtual_corpus_type_index;
DROP INDEX IF EXISTS  virtual_corpus_unique_name; 

CREATE INDEX IF NOT EXISTS query_owner_index ON query(created_by);
CREATE INDEX IF NOT EXISTS query_type_index ON query(type);
CREATE UNIQUE INDEX IF NOT EXISTS  query_unique_name 
	ON query(name,created_by);



ALTER TABLE virtual_corpus_access 
RENAME COLUMN virtual_corpus_id TO query_id;

ALTER TABLE virtual_corpus_access
RENAME TO query_access;




DROP TABLE IF EXISTS query_reference;

DROP INDEX IF EXISTS query_reference_owner_index;
DROP INDEX IF EXISTS query_reference_type_index;
DROP INDEX IF EXISTS  query_reference_unique_name;