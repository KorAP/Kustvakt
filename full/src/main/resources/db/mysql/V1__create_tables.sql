
CREATE TABLE IF NOT EXISTS annotation(
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	code VARCHAR(20) NOT NULL,
	type VARCHAR(20) NOT NULL,
	text VARCHAR(20) NULL,
	description VARCHAR(100) NOT NULL,
	de_description VARCHAR(100),
	UNIQUE INDEX unique_index (code, type)
);

CREATE TABLE IF NOT EXISTS annotation_layer(
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	foundry_id INTEGER NOT NULL,
	layer_id INTEGER NOT NULL,
	description VARCHAR(300) NOT NULL,
	UNIQUE INDEX unique_index (foundry_id, layer_id),
	FOREIGN KEY (foundry_id)
		REFERENCES annotation (id)
		ON DELETE CASCADE,
	FOREIGN KEY (layer_id)
		REFERENCES annotation (id)
		ON DELETE CASCADE
	
);

CREATE TABLE IF NOT EXISTS annotation_key(
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	layer_id INTEGER NOT NULL,
	key_id INTEGER NOT NULL,
	UNIQUE INDEX unique_index (layer_id, key_id),
	FOREIGN KEY (layer_id)
		REFERENCES annotation_layer (id)
		ON DELETE CASCADE,
	FOREIGN KEY (key_id)
		REFERENCES annotation (id)
		ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS annotation_value(
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	key_id INTEGER NOT NULL,
	value_id INTEGER NOT NULL,
	UNIQUE INDEX unique_index(key_id, value_id),
	FOREIGN KEY (key_id)
		REFERENCES annotation_key (id)
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
		REFERENCES annotation_layer (id)
		ON DELETE CASCADE	
);

