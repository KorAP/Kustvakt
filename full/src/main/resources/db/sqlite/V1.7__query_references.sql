CREATE TABLE IF NOT EXISTS query_reference (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name VARCHAR(255) NOT NULL,
  type VARCHAR(100) NOT NULL,
  required_access VARCHAR(100) NOT NULL,
  created_by VARCHAR(100) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  status VARCHAR(100) DEFAULT NULL,
  query TEXT NOT NULL,
  definition VARCHAR(255) DEFAULT NULL
);

CREATE INDEX query_reference_owner_index ON query_reference(created_by);
CREATE INDEX query_reference_type_index ON query_reference(type);
CREATE UNIQUE INDEX query_reference_unique_name 
	ON query_reference(name,created_by);
