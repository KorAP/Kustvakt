CREATE TABLE IF NOT EXISTS korap_users (
id INTEGER PRIMARY KEY AUTOINCREMENT,
username VARCHAR(150) NOT NULL UNIQUE,
password VARCHAR(100) NOT NULL,
accountLock boolean NOT NULL,
accountCreation TIMESTAMP NOT NULL,
-- deprecate this
type INTEGER DEFAULT 0,
uri_fragment VARCHAR(100),
uri_expiration TIMESTAMP,
accountLink VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS shib_users (
id INTEGER PRIMARY KEY AUTOINCREMENT,
username VARCHAR(150) NOT NULL UNIQUE,
accountCreation TIMESTAMP NOT NULL,
type INTEGER DEFAULT 1,
loginSuccess INTEGER,
loginFailed INTEGER,
accountLink VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS user_details (
id INTEGER PRIMARY KEY AUTOINCREMENT,
user_id INTEGER NOT NULL,
firstName VARCHAR(100),
lastName VARCHAR(100),
gender VARCHAR(100),
phone VARCHAR(100),
institution VARCHAR(100),
email VARCHAR(100),
address VARCHAR(100),
country VARCHAR(100),
privateUsage BOOLEAN,
foreign key (user_id)
references korap_users (id)
on delete cascade
);

CREATE TABLE IF NOT EXISTS user_settings (
id INTEGER PRIMARY KEY AUTOINCREMENT,
user_id INTEGER NOT NULL,
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
collectData BOOLEAN,
foreign key (user_id)
references korap_users (id)
on delete cascade
);


CREATE TABLE IF NOT EXISTS user_queries (
id INTEGER PRIMARY KEY,
queryLanguage VARCHAR(100),
name VARCHAR(100),
query VARCHAR(200),
description VARCHAR(150),
foreign key (id)
references resource_store (id)
on delete cascade
);

CREATE TABLE IF NOT EXISTS coll_store (
id INTEGER PRIMARY KEY AUTOINCREMENT,
persistent_id VARCHAR(150) UNIQUE,
name VARCHAR(150),
description VARCHAR(200),
query VARCHAR(500),
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
user_id INTEGER,
foreign key(user_id)
references korap_users(id)
on delete cascade
);

CREATE TABLE IF NOT EXISTS audit_records (
aud_id INTEGER PRIMARY KEY AUTOINCREMENT,
aud_category VARCHAR(100),
aud_target VARCHAR(100),
aud_user VARCHAR(100),
aud_location VARCHAR(100),
aud_field_1 VARCHAR(400),
aud_args VARCHAR(400),
aud_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
aud_status VARCHAR(100)
);

-- deprecated
CREATE TABLE IF NOT EXISTS match_info (
id INTEGER PRIMARY KEY AUTOINCREMENT,
userid BIGINT NOT NULL,
matchInfo VARCHAR(100)
);


CREATE TABLE IF NOT EXISTS policy_store (
id INTEGER PRIMARY KEY AUTOINCREMENT,
target_id BIGINT NOT NULL,
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
creator INTEGER NOT NULL,
posix SMALLINT NOT NULL,
expire timestamp,
enable timestamp NOT NULL,
iprange varchar(200)
);

-- send disabled documents per corpus to backend, so they can be excluded from searching!
CREATE TABLE IF NOT EXISTS doc_store (
id INTEGER PRIMARY KEY AUTOINCREMENT,
persistent_id VARCHAR(265) UNIQUE,
created DATE DEFAULT CURRENT_TIMESTAMP,
disabled BOOLEAN default true
);

CREATE TABLE IF NOT EXISTS group_ref (
id INTEGER PRIMARY KEY AUTOINCREMENT,
group_id VARCHAR(100) NOT NULL,
policy_id INTEGER NOT NULL
);

-- question: grouping of users or grouping of resources required?
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
id INTEGER PRIMARY KEY AUTOINCREMENT,
user_id INTEGER NOT NULL,
group_id VARCHAR(100) NOT NULL,
admin BOOLEAN NOT NULL DEFAULT FALSE,
FOREIGN KEY (user_id)
REFERENCES korap_users(id)
on delete cascade,
FOREIGN KEY (group_id)
REFERENCES group_store (name)
on delete cascade
);


CREATE TABLE IF NOT EXISTS param_store (
id INTEGER PRIMARY KEY AUTOINCREMENT,
p_key VARCHAR(150) NOT NULL,
p_value VARCHAR(200) NOT NULL,
resource INTEGER NOT NULL DEFAULT -1,
pid INTEGER NOT NULL DEFAULT -1,
FOREIGN KEY (resource)
REFERENCES resource_store(id)
on delete cascade,
FOREIGN KEY (pid)
REFERENCES policy_store(id)
on delete cascade
);

CREATE TABLE IF NOT EXISTS param_map (
id INTEGER PRIMARY KEY AUTOINCREMENT,
policyId INTEGER NOT NULL,
paramId INTEGER NOT NULL,
value VARCHAR(100) NOT NULL,
flag BOOLEAN NOT NULL DEFAULT FALSE,
FOREIGN KEY (paramId)
   REFERENCES param_store (id),
FOREIGN KEY (policyId)
   REFERENCES policy_store (id)
);

CREATE TABLE IF NOT EXISTS resource_store (
id INTEGER PRIMARY KEY AUTOINCREMENT,
persistent_id VARCHAR(100) NOT NULL UNIQUE,
name VARCHAR(100),
description VARCHAR(300),
parent_id Integer unsigned null,
created timestamp default current_timestamp,
type INTEGER NOT NULL,
creator INTEGER NOT NULL
);


CREATE TABLE IF NOT EXISTS resource_tree (
parent_id INTEGER,
child_id INTEGER,
depth INTEGER,
name_path VARCHAR(250),
PRIMARY KEY (parent_id, child_id),
foreign key (parent_id)
references resource_store (id)
on delete cascade,
foreign key (child_id)
references resource_store (id)
on delete cascade
);


-- todo: refactor native to confidential, add column application name
-- remove id and make client_id primary key
create table IF NOT EXISTS oauth2_client (
id INTEGER PRIMARY KEY AUTOINCREMENT,
client_id VARCHAR(100) NOT NULL,
client_secret VARCHAR(200) NOT NULL,
redirect_uri VARCHAR(250) NOT NULL,
client_type VARCHAR(200),
is_confidential BOOLEAN DEFAULT FALSE,
url VARCHAR(200) UNIQUE
);


-- refresh token doesn't care about expiration.
-- also narrower scopes for new access token with the refresh token are not supported
-- otherwise i would require a comparison of all access_token to get the maximum scopes and compare to request

-- status 1 = valid, 0 = revoked, -1 = disabled
create table IF NOT EXISTS oauth2_access_token (
id INTEGER PRIMARY KEY AUTOINCREMENT,
access_token VARCHAR(300),
auth_code VARCHAR(250),
refresh_token VARCHAR(250),
client_id VARCHAR(100),
user_id INTEGER,
-- make boolean --
status INTEGER DEFAULT 1,
-- in case of code authorization, should match auth code scopes!
-- use scopes for levelaccess descriptor level[rw],level[r]
scopes VARCHAR(350),
expiration TIMESTAMP,
FOREIGN KEY (user_id)
REFERENCES korap_users(id),
FOREIGN KEY (client_id)
REFERENCES oauth2_client(client_id)
);


-- fixme: also scopes?
create table oauth2_refresh_token (
id INTEGER PRIMARY KEY AUTOINCREMENT,
client_id VARCHAR(100),
user_id INTEGER,
expiration TIMESTAMP,
scopes VARCHAR(350),
FOREIGN KEY (user_id)
REFERENCES korap_users(id)
ON DELETE CASCADE,
FOREIGN KEY (client_id)
REFERENCES oauth2_client(client_id)
ON DELETE CASCADE
);



-- a bit confusing. 1. creator is policy creator, 2. creator is resource creator --> different implications
-- insert resource data from resource_store alltogether, so i dont have to retrieve anything from there?!
create view if not exists policy_view as
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
from policy_store as po
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

-- deletes a group if the group has no longer members!
create trigger if not exists group_ref_del after delete on group_ref
when (select count(*) from group_ref where groupId=OLD.group_id) = 0
begin delete from groupolicy_store where name=OLD.group_id; end;

    -- create trigger relCr after insert on resource_store
    -- when (select count(*) from r_tree where parent_id==NEW.id and
    -- child_id==NEW.id) == 0
    -- BEGIN
    -- insert into r_tree (parent_id, child_id, depth)
    -- VALUES (NEW.id, NEW.id, 0);
    -- END;

    -- 1. CONCAT(NEW.name,"/")
    -- 2. CONCAT(name_path, NEW.name, "/")

CREATE TRIGGER IF NOT EXISTS insert_data
AFTER INSERT ON resource_store
FOR EACH ROW BEGIN
INSERT INTO resource_tree (parent_id, child_id, depth, name_path)
VALUES (NEW.id, NEW.id, 0, NEW.name);

INSERT INTO resource_tree (parent_id, child_id, depth, name_path)
SELECT parent_id, NEW.id, depth + 1, name_path || "/" ||  NEW.name FROM resource_tree
WHERE child_id = NEW.parent_id;
END;

create trigger if not exists delete_policy after delete on resource_store
begin delete from policy_store where target_id=OLD.id; end;

    -- 1. requirement: delete hierarchical from resource_store and r_tree -- done!
    -- 2. todo: subsequently delete from resourcedao extensions if child of deleted resource!
create trigger if not exists del_tree after delete on resource_store
begin delete from resource_store where id in (select rs.id from resource_store as rs
inner join resource_tree as rt on rt.child_id=rs.id where rt.parent_id=OLD.id);
delete from resource_tree where parent_id=OLD.id; end;

-- mysql on delete cascade todo: test
create trigger if not exists del_user delete on korap_users
begin
    delete from user_settings where user_id=OLD.id;
    delete from user_details where user_id=OLD.id;
    delete from group_users where user_id=OLD.id;
end;

-- indices
-- test unique index constraints
create index group_index on group_users(user_id);
create index policy_index on group_ref(policy_id);
create index policy_target on policy_store(target_id);
create unique index r_tree_index on resource_tree (parent_id, depth, child_id);
create unique index para_unique on param_store (p_key, p_value);
create unique index conditions on group_ref (policy_id, group_id);
create unique index groups on group_users (user_id, group_id);


-- deprecated
-- flagr is a reference to the applicable conditions: export, licence
create table if not exists policy_store2 (
id integer primary key autoincrement,
target_id bigint not null,
creator bigint not null,
perm integer default -1,
enable boolean default true,
master INTEGER UNIQUE default NULL,
expire timestamp default null,
iprange varchar(200),
flagr integer,
params integer,
baseline boolean default false,
FOREIGN KEY (master)
    REFERENCES policy_store2(id),
FOREIGN KEY (flagr)
    REFERENCES flag_store (id),
FOREIGN KEY (params)
    REFERENCES param_store (id)
);

-- grouping is matched with a view where the user and groups are listed together
create table if not exists flag_store (
id integer primary key autoincrement,
export boolean default true,
sym_use integer default -1,
grouping varchar(150),
FOREIGN KEY (grouping)
 REFERENCES groupings (grouping)
);

-- todo: ??!
-- haveing the username as grouping only works with the unique identifier at username
create view if not exists groupings_view
as select id as user_id, username as grouping from korap_users
union all select user_id, group_id as grouping from group_users;
