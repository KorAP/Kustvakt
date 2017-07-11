
--type
--0	value
--1	foundry
--2	layer
--3	key
CREATE TABLE IF NOT EXISTS annotation(
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	symbol VARCHAR(20) NOT NULL,
	type INTEGER DEFAULT 0,	
	description VARCHAR(100) NOT NULL,
	UNIQUE INDEX unique_index (symbol, type)
);

CREATE TABLE IF NOT EXISTS annotation_pair(
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	annotation1 INTEGER NOT NULL,
	annotation2 INTEGER NOT NULL,
	description VARCHAR(300) NOT NULL,
	german_description VARCHAR(300),
	UNIQUE INDEX unique_index (annotation1, annotation2),
	FOREIGN KEY (annotation1)
		REFERENCES annotation (id)
		ON DELETE CASCADE,
	FOREIGN KEY (annotation2)
		REFERENCES annotation (id)
		ON DELETE CASCADE
	
);

CREATE TABLE IF NOT EXISTS annotation_pair_value(
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	pair_id INTEGER NOT NULL,
	value INTEGER NOT NULL,
	UNIQUE INDEX unique_index (pair_id, value),
	FOREIGN KEY (pair_id)
		REFERENCES annotation_pair (id)
		ON DELETE CASCADE,
	FOREIGN KEY (value)
		REFERENCES annotation (id)
		ON DELETE CASCADE
);

CREATE TABLE resource(
	id VARCHAR(100) PRIMARY KEY UNIQUE NOT NULL,
	title VARCHAR(100) NOT NULL,
	en_title VARCHAR(100) NOT NULL,
	description VARCHAR(100)	
);

CREATE TABLE resource_layer(
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	resource_id VARCHAR(100) NOT NULL,
	layer_id INTEGER NOT NULL,
	UNIQUE INDEX pair_index (resource_id, layer_id),
	FOREIGN KEY (resource_id)
		REFERENCES resource (id)
		ON DELETE CASCADE,
	FOREIGN KEY (layer_id)
		REFERENCES annotation_pair (id)
		ON DELETE CASCADE	
);

