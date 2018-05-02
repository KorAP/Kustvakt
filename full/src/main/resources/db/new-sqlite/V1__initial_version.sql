CREATE TABLE IF NOT EXISTS annotation(
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	code VARCHAR(20) NOT NULL,
	type VARCHAR(20) NOT NULL,	
	description VARCHAR(100) NOT NULL,
	de_description VARCHAR(100)
);

CREATE UNIQUE INDEX annotation_index ON annotation (code, type);

CREATE TABLE IF NOT EXISTS annotation_pair(
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	annotation1 INTEGER NOT NULL,
	annotation2 INTEGER NOT NULL,
	description VARCHAR(255) NOT NULL,
	FOREIGN KEY (annotation1)
		REFERENCES annotation (id)
		ON DELETE CASCADE,
	FOREIGN KEY (annotation2)
		REFERENCES annotation (id)
		ON DELETE CASCADE
	
);

CREATE UNIQUE INDEX annotation_pair_index ON annotation_pair (annotation1, annotation2);

CREATE TABLE IF NOT EXISTS annotation_pair_value(
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	pair_id INTEGER NOT NULL,
	value_id INTEGER NOT NULL,
	FOREIGN KEY (pair_id)
		REFERENCES annotation_pair (id)
		ON DELETE CASCADE,
	FOREIGN KEY (value_id)
		REFERENCES annotation (id)
		ON DELETE CASCADE
);

CREATE UNIQUE INDEX annotation_pair_value_index ON annotation_pair_value (pair_id, value_id);

CREATE TABLE resource(
	id VARCHAR(100) PRIMARY KEY UNIQUE NOT NULL,
	de_title VARCHAR(100) NOT NULL,
	en_title VARCHAR(100) NOT NULL,
	en_description VARCHAR(100)	
);

CREATE TABLE resource_layer(
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	resource_id VARCHAR(100) NOT NULL,
	layer_id INTEGER NOT NULL,
	FOREIGN KEY (resource_id)
		REFERENCES resource (id)
		ON DELETE CASCADE,
	FOREIGN KEY (layer_id)
		REFERENCES annotation_pair (id)
		ON DELETE CASCADE	
);

CREATE UNIQUE INDEX resource_layer_index ON resource_layer (resource_id, layer_id);

