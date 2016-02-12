-- last_modified timestamp ON UPDATE CURRENT_TIMESTAMP,
CREATE TABLE IF NOT EXISTS policy_store (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    target_id BIGINT NOT NULL,
    created TIMESTAMP,
    creator INTEGER NOT NULL,
    posix SMALLINT NOT NULL,
    expire BIGINT NULL,
    enable BIGINT NULL,
    iprange VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS group_ref (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    group_id VARCHAR(100) NOT NULL,
    policy_id INTEGER NOT NULL
);


CREATE TABLE IF NOT EXISTS group_store (
    name VARCHAR(100) PRIMARY KEY,
    description VARCHAR(200),
    sym_use INTEGER DEFAULT -1,
    export VARCHAR(30) DEFAULT NULL,
    query_only VARCHAR(30) DEFAULT NULL,
    licence INTEGER DEFAULT -1,
    -- basically every resource we have is an academic resource, thus a non-commercial use is infered!
    commercial BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS group_users (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    -- make integer
    group_id VARCHAR(100) NOT NULL,
    admin BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (group_id)
        REFERENCES group_store (name) on delete cascade
);

CREATE TABLE IF NOT EXISTS param_store (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    p_key VARCHAR(100) NOT NULL,
    p_value VARCHAR(150) NOT NULL,
    resource INTEGER DEFAULT -1,
    pid INTEGER DEFAULT -1,
    FOREIGN KEY (resource)
        REFERENCES resource_store(id)
    on delete cascade,
    FOREIGN KEY (pid)
        REFERENCES policy_store(id)
    on delete cascade
);

CREATE TABLE IF NOT EXISTS param_map (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    policy_id INTEGER NOT NULL,
    param_id INTEGER NOT NULL,
    value VARCHAR(100) NOT NULL,
    flag BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (param_id)
        REFERENCES param_store (id),
    FOREIGN KEY (policy_id)
        REFERENCES policy_store (id)
);

create or replace view policy_view as
select
    po.id as pid,
    po.target_id as id,
    rs.persistent_id as persistent_id,
    rs.name as name,
    rs.type as type,
    c.group_id as group_id,
    po.posix as perm,
    po.creator as creator,
    po.expire as expire,
    po.enable as enable,
    po.iprange as iprange
from
policy_store as po
inner join
group_ref as c ON c.policy_id = po.id
inner join
resource_store as rs ON rs.id = po.target_id
union all select
              - 1 as pid,
              rs.id as id,
              rs.persistent_id as persistent_id,
              rs.name as name,
              type as type,
              'self' as group_id,
              127 as perm,
              creator,
              NULL as expire,
              rs.created as enable,
              null as iprange
          from
          resource_store as rs;


CREATE TABLE IF NOT EXISTS audit_records (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
aud_category VARCHAR(100),
aud_target VARCHAR(100),
aud_user VARCHAR(100),
aud_location VARCHAR(100),
aud_operation VARCHAR(100),
aud_field_1 VARCHAR(400),
aud_timestamp TIMESTAMP,
aud_failure VARCHAR(100)
);


-- indices
create trigger delete_policy after delete on resource_store
for each row delete from policy_store where target_id=OLD.id;

DELIMITER //
CREATE TRIGGER tree_entry_insert AFTER INSERT ON resource_store FOR EACH ROW BEGIN
	INSERT INTO resource_tree (parent_id, child_id, depth, name_path)
	VALUES (NEW.id, NEW.id, 0, NEW.name);
	INSERT INTO resource_tree (parent_id, child_id, depth, name_path)
	SELECT parent_id, NEW.id, depth + 1, concat(name_path,"/",NEW.name) FROM resource_tree WHERE child_id = NEW.parent_id;
END; //

DELIMITER ;

-- todo: are this automatically adapted when refactoring?
CREATE INDEX group_index ON group_users(user_id, group_id);
CREATE INDEX policy_index ON group_ref(policy_id);
CREATE UNIQUE INDEX resource_tree_index ON resource_tree (parent_id, depth, child_id);
CREATE UNIQUE INDEX param_unique ON param_store (p_key, p_value);

-- foreign key constraints
