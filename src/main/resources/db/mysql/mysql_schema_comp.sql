CREATE TABLE IF NOT EXISTS korap_users (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    accountLock boolean NOT NULL,
    accountCreation TIMESTAMP NOT NULL,
    type INTEGER DEFAULT 0,
    uriFragment VARCHAR(100),
    uriExpiration TIMESTAMP,
    accountLink VARCHAR(100)
)$$


CREATE TABLE IF NOT EXISTS shib_users (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    accountCreation TIMESTAMP NOT NULL,
    type INTEGER DEFAULT 1,
    loginSuccess INTEGER,
    loginFailed INTEGER,
    accountExpiration TIMESTAMP NOT NULL,
    accountLink VARCHAR(100)
)$$

CREATE TABLE IF NOT EXISTS admin_users (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER NOT NULL,
    foreign key (user_id)
    references korap_users (id)
)$$

CREATE TABLE IF NOT EXISTS user_details (
    Id INTEGER PRIMARY KEY AUTO_INCREMENT,
    userID INTEGER NOT NULL UNIQUE,
    firstName VARCHAR(100),
    lastName VARCHAR(100),
    gender VARCHAR(100),
    phone VARCHAR(100),
    institution VARCHAR(100),
    email VARCHAR(100),
    address VARCHAR(100),
    country VARCHAR(100),
    privateUsage BOOLEAN,
    foreign key (userId)
    references korap_users (id)
    on delete cascade
)$$

CREATE TABLE IF NOT EXISTS user_settings (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
userId INTEGER NOT NULL,
fileNameForExport VARCHAR(100),
leftContextItemForExport VARCHAR(100),
leftContextSizeForExport INTEGER,
locale VARCHAR(100),
leftContextItem VARCHAR(100),
leftContextSize INTEGER,
rightContextItem VARCHAR(100),
rightContextItemForExport VARCHAR(100),
rightContextSize INTEGER,
rightContextSizeForExport INTEGER,
selectedCollection VARCHAR(100),
queryLanguage VARCHAR(100),
pageLength INTEGER,
metadataQueryExpertModus BOOLEAN,
POSFoundry VARCHAR(100),
lemmaFoundry VARCHAR(100),
constFoundry VARCHAR(100),
relFoundry VARCHAR(100),
collectData BOOLEAN DEFAULT TRUE,
foreign key (userId)
references korap_users (id)
on delete cascade
)$$

CREATE TABLE IF NOT EXISTS user_queries (
    id INTEGER PRIMARY KEY,
    queryLanguage VARCHAR(100),
    name VARCHAR(100),
    query VARCHAR(200),
    description VARCHAR(150),
    foreign key (id)
    references resource_store(id)
    on delete cascade)$$


CREATE TABLE IF NOT EXISTS resource_store (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
persistentID VARCHAR(100) NOT NULL UNIQUE,
name VARCHAR(100),
description VARCHAR(300),
parentID Integer unsigned null,
created timestamp default current_timestamp,
type INTEGER NOT NULL,
creator INTEGER NOT NULL
)$$


CREATE TABLE IF NOT EXISTS resource_tree (
parentID INTEGER,
childID INTEGER,
depth INTEGER,
name_path VARCHAR(250),
PRIMARY KEY (parentID , childID),
foreign key (parentID)
references resource_store (id)
on delete cascade,
foreign key (childID)
references resource_store (id)
on delete cascade
)$$


CREATE TABLE IF NOT EXISTS coll_store (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
persistentID VARCHAR(150) UNIQUE,
name VARCHAR(150),
description VARCHAR(200),
query VARCHAR(500),
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
userID INTEGER,
foreign key(userID)
references korap_users(id)
on delete cascade
)$$



CREATE TABLE IF NOT EXISTS match_info (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
userid BIGINT NOT NULL,
match_info VARCHAR(100)
)$$

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
)$$

CREATE TABLE IF NOT EXISTS doc_store (
id VARCHAR(265) PRIMARY KEY,
persistentID VARCHAR(265) UNIQUE,
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
disabled BOOLEAN default true
)$$

-- last_modified timestamp ON UPDATE CURRENT_TIMESTAMP,
CREATE TABLE IF NOT EXISTS policy_store (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    targetID BIGINT NOT NULL,
    created TIMESTAMP,
    creator INTEGER NOT NULL,
    posix SMALLINT NOT NULL,
    expire timestamp null,
    enable timestamp not null,
    iprange VARCHAR(200)
)$$

CREATE TABLE IF NOT EXISTS group_ref (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    groupId VARCHAR(100) NOT NULL,
    policyid INTEGER NOT NULL
)$$


CREATE TABLE IF NOT EXISTS group_store (
    name VARCHAR(100) PRIMARY KEY,
    description VARCHAR(200),
    sym_use INTEGER DEFAULT -1,
    export VARCHAR(30) DEFAULT NULL,
    query_only VARCHAR(30) DEFAULT NULL,
    licence INTEGER DEFAULT -1,
    -- basically every resource we have is an academic resource, thus a non-commercial use is infered!
    commercial BOOLEAN DEFAULT FALSE
)$$

CREATE TABLE IF NOT EXISTS group_users (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    userID BIGINT NOT NULL,
    groupId VARCHAR(100) NOT NULL,
    admin BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (groupId)
        REFERENCES groupStore (name) on delete cascade
)$$

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
)$$

CREATE TABLE IF NOT EXISTS param_map (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    policyID INTEGER NOT NULL,
    paramID INTEGER NOT NULL,
    value VARCHAR(100) NOT NULL,
    flag BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (paramID)
        REFERENCES param_store (id),
    FOREIGN KEY (policyID)
        REFERENCES policy_store (id)
)$$

create or replace view policy_view as
select
    po.id as pid,
    po.targetID as id,
    rs.persistentID as persistentID,
    rs.name as name,
    rs.type as type,
    c.groupId as groupId,
    po.posix as perm,
    po.creator as creator,
    po.expire as expire,
    po.enable as enable,
    po.iprange as iprange    
from
policy_store as po
inner join
group_ref as c ON c.policyid = po.id
inner join
resource_store as rs ON rs.id = po.targetid
union all select
              - 1 as pid,
              rs.id as id,
              rs.persistentID as persistentID,
              rs.name as name,
              type as type,
              'self' as groupId,
              127 as perm,
              creator,              
              NULL as expire,
              rs.created as enable,
              null as iprange
          from
          resource_store as rs$$



-- oauth2 db tables
create table oauth2_client (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
client_id VARCHAR(100),
client_secret VARCHAR(200),
redirect_uri VARCHAR(250),
client_type VARCHAR(200),
native BOOLEAN DEFAULT FALSE,
url VARCHAR(200) UNIQUE
)$$

create table oauth2_auth_codes (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
client_id VARCHAR(100),
auth_code VARCHAR(250),
status INTEGER DEFAULT 1,
scopes VARCHAR (150),
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)$$

create table oauth2_client_authorization (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
client_id INTEGER,
user_id INTEGER,
-- define scopes?! --
FOREIGN KEY (client_id) REFERENCES oauth2_client(client_id)
)$$

-- status 1 = valid, 0 = revoked
create table oauth2_access_token (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
access_token VARCHAR(300),
auth_code VARCHAR(250),
userId INTEGER,
status INTEGER DEFAULT 1,
expiration TIMESTAMP,
FOREIGN KEY (userId)
REFERENCES korap_users(id)
)$$



-- indices
create trigger delete_policy after delete on resource_store
for each row delete from policy_store where targetID=OLD.id$$

CREATE TRIGGER insert_data AFTER INSERT ON resource_store FOR EACH ROW BEGIN
	INSERT INTO resource_tree (parentID, childID, depth, name_path)
	VALUES (NEW.id, NEW.id, 0, NEW.name);
	INSERT INTO resource_tree (parentID, childID, depth, name_path)
	SELECT parentID, NEW.id, depth + 1, concat(name_path,"/",NEW.name) FROM resource_tree WHERE childID = NEW.parentID;
END$$

CREATE INDEX group_index ON group_users(userid)$$
CREATE INDEX policy_index ON group_ref(policyid)$$
CREATE UNIQUE INDEX resource_tree_index on resource_tree (parentID, depth, childID)$$
CREATE UNIQUE INDEX para_unique ON param_store (p_key, p_value)$$










