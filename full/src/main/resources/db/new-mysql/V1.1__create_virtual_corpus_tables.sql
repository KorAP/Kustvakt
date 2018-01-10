CREATE TABLE IF NOT EXISTS role (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  UNIQUE INDEX name_index(name)
);


CREATE TABLE IF NOT EXISTS privilege (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  name varchar(20) NOT NULL,
  role_id int NOT NULL,
  UNIQUE INDEX privilege_index(name, role_id),
  FOREIGN KEY (role_id) 
  	REFERENCES role (id)
  	ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS user_group (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  status varchar(100) NOT NULL,
  created_by varchar(100) NOT NULL,
  deleted_by varchar(100) DEFAULT NULL,
  INDEX status_index(status)
);

CREATE TABLE IF NOT EXISTS user_group_member (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  user_id varchar(100) NOT NULL,
  group_id int(11) NOT NULL,
  status varchar(100) NOT NULL,
  created_by varchar(100) NOT NULL,
  deleted_by varchar(100) DEFAULT NULL,
  UNIQUE INDEX unique_index (user_id,group_id),
  INDEX status_index(status),
  FOREIGN KEY (group_id) 
  	REFERENCES user_group (id)
  	ON DELETE CASCADE
); 

CREATE TABLE IF NOT EXISTS group_member_role (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  group_member_id int(11) NOT NULL,
  role_id int NOT NULL,
  UNIQUE INDEX unique_index (group_member_id,role_id),
  FOREIGN KEY (group_member_id)
  	REFERENCES user_group_member (id)
  	ON DELETE CASCADE,
  FOREIGN KEY (role_id) 
  	REFERENCES role (id)
  	ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS virtual_corpus (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  name varchar(255) NOT NULL,
  type varchar(100) NOT NULL,
  required_access varchar(100) NOT NULL,
  created_by varchar(100) NOT NULL,
  description varchar(255) DEFAULT NULL,
  status varchar(100) DEFAULT NULL,
  corpus_query varchar(2000) NOT NULL,
  definition varchar(255) DEFAULT NULL,
  INDEX owner_index (created_by),
  INDEX type_index (type)
);

CREATE TABLE IF NOT EXISTS virtual_corpus_access (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  virtual_corpus_id int(11) NOT NULL,
  user_group_id int(11) NOT NULL,
  status varchar(100) NOT NULL,
  created_by varchar(100) NOT NULL,
  approved_by varchar(100) DEFAULT NULL,
  deleted_by varchar(100) DEFAULT NULL,
  UNIQUE INDEX unique_index (virtual_corpus_id,user_group_id),
  INDEX status_index(status),
  FOREIGN KEY (user_group_id) 
  	REFERENCES user_group (id)
  	ON DELETE CASCADE,
  FOREIGN KEY (virtual_corpus_id) 
  	REFERENCES virtual_corpus (id)
  	ON DELETE CASCADE
);