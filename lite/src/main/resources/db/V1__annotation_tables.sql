CREATE TABLE IF NOT EXISTS annotation(
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	code VARCHAR(20) NOT NULL,
	type VARCHAR(20) NOT NULL,
	text VARCHAR(20) NULL,
	description VARCHAR(100) NOT NULL,
	de_description VARCHAR(100)
);

CREATE UNIQUE INDEX annotation_index ON annotation (code, type);

CREATE TABLE IF NOT EXISTS annotation_layer(
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	foundry_id INTEGER NOT NULL,
	layer_id INTEGER NOT NULL,
	description VARCHAR(255) NOT NULL,
	FOREIGN KEY (foundry_id)
		REFERENCES annotation (id)
		ON DELETE CASCADE,
	FOREIGN KEY (layer_id)
		REFERENCES annotation (id)
		ON DELETE CASCADE
);

CREATE UNIQUE INDEX annotation_layer_index ON annotation_layer (foundry_id, layer_id);

CREATE TABLE IF NOT EXISTS annotation_key(
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	layer_id INTEGER NOT NULL,
	key_id INTEGER NOT NULL,
	FOREIGN KEY (layer_id)
		REFERENCES annotation_layer (id)
		ON DELETE CASCADE,
	FOREIGN KEY (key_id)
		REFERENCES annotation (id)
		ON DELETE CASCADE
);

CREATE UNIQUE INDEX annotation_key_index ON annotation_key (layer_id, key_id);

CREATE TABLE IF NOT EXISTS annotation_value(
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	key_id INTEGER NOT NULL,
	value_id INTEGER NOT NULL,
	FOREIGN KEY (key_id)
		REFERENCES annotation_key (id)
		ON DELETE CASCADE,
	FOREIGN KEY (key_id)
		REFERENCES annotation (id)
		ON DELETE CASCADE
);

CREATE UNIQUE INDEX annotation_value_index ON annotation_value (key_id, value_id);
