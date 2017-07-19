
CREATE TABLE IF NOT EXISTS annotation(
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	code VARCHAR(20) NOT NULL,
	type VARCHAR(20) NOT NULL,	
	description VARCHAR(100) NOT NULL,
	UNIQUE INDEX unique_index (code, type)
);

CREATE TABLE IF NOT EXISTS annotation_pair(
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	annotation1 INTEGER NOT NULL,
	annotation2 INTEGER NOT NULL,
	description VARCHAR(300) NOT NULL,
	de_description VARCHAR(300),
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
	value_id INTEGER NOT NULL,
	UNIQUE INDEX unique_index (pair_id, value_id),
	FOREIGN KEY (pair_id)
		REFERENCES annotation_pair (id)
		ON DELETE CASCADE,
	FOREIGN KEY (value_id)
		REFERENCES annotation (id)
		ON DELETE CASCADE
);

CREATE TABLE resource(
	id VARCHAR(100) PRIMARY KEY UNIQUE NOT NULL,
	de_title VARCHAR(100) NOT NULL,
	en_title VARCHAR(100) NOT NULL,
	en_description VARCHAR(100)	
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

