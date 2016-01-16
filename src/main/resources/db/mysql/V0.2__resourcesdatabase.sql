-- why unsigned?
CREATE TABLE IF NOT EXISTS resource_store (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
persistent_id VARCHAR(100) NOT NULL UNIQUE,
name VARCHAR(100),
description VARCHAR(300),
parent_id Integer unsigned null,
created BIGINT NOT NULL,
type INTEGER NOT NULL,
creator INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS resource_tree (
parent_id INTEGER,
child_id INTEGER,
depth INTEGER,
name_path VARCHAR(250),
PRIMARY KEY (parent_id , child_id),
foreign key (parent_id)
references resource_store (id)
on delete cascade,
foreign key (child_id)
references resource_store (id)
on delete cascade
);


CREATE TABLE IF NOT EXISTS user_queries (
    id INTEGER PRIMARY KEY,
    queryLanguage VARCHAR(100),
    name VARCHAR(100),
    query VARCHAR(200),
    description VARCHAR(150),
    foreign key (id)
    references resource_store(id)
    on delete cascade
);

CREATE TABLE IF NOT EXISTS coll_store (
    id INTEGER,
    query VARCHAR(500),
    user_id INTEGER,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- is foreign key constraint valid after refactoring?
    foreign key (id) references resource_store(id)
    on delete cascade);


CREATE TABLE IF NOT EXISTS matchInfo (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    matchInfo VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS doc_store (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    persistent_id VARCHAR(100) UNIQUE,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    disabled BOOLEAN default true
);