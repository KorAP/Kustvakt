ALTER TABLE virtual_corpus 
ADD COLUMN query_type VARCHAR(100) NOT NULL;

ALTER TABLE virtual_corpus 
ADD COLUMN query TEXT DEFAULT NULL;

ALTER TABLE virtual_corpus 
ADD COLUMN query_language VARCHAR(100) DEFAULT NULL;


DROP TABLE IF EXISTS query_reference;

DROP INDEX IF EXISTS query_reference_owner_index;
DROP INDEX IF EXISTS query_reference_type_index;
DROP INDEX IF EXISTS  query_reference_unique_name;