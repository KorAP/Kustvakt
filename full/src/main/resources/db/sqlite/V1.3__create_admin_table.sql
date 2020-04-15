CREATE TABLE IF NOT EXISTS admin(
	id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
	user_id VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS admin_index on admin(user_id);