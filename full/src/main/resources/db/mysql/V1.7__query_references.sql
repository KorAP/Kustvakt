CREATE TABLE IF NOT EXISTS query_refernce (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  type VARCHAR(100) NOT NULL,
  required_access VARCHAR(100) NOT NULL,
  created_by VARCHAR(100) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  status VARCHAR(100) DEFAULT NULL,
  query TEXT NOT NULL,
  definition VARCHAR(255) DEFAULT NULL,
  UNIQUE INDEX unique_index (name,created_by),
  INDEX owner_index (created_by),
  INDEX type_index (type)
);
