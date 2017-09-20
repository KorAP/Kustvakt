--
---- rename all columns in new way!
--CREATE TABLE IF NOT EXISTS korapusers (
--    id INTEGER PRIMARY KEY AUTO_INCREMENT,
--    username VARCHAR(100) NOT NULL UNIQUE,
--    password VARCHAR(100) NOT NULL,
--    accountLock boolean NOT NULL,
--    accountCreation TIMESTAMP NOT NULL,
--    type INTEGER DEFAULT 0,
--    URI_PASS_Fragment VARCHAR(100),
--    URI_CONF_Fragment VARCHAR(100),
--    URI_Expiration TIMESTAMP,
--    loginSuccess INTEGER,
--    loginFailed INTEGER,
--    accountExpiration TIMESTAMP NOT NULL,
--    accountLink VARCHAR(100)
--);
--
--CREATE TABLE IF NOT EXISTS shibusers (
--    id INTEGER PRIMARY KEY AUTO_INCREMENT,
--    username VARCHAR(100) NOT NULL UNIQUE,
--    accountCreation TIMESTAMP NOT NULL,
--    type INTEGER DEFAULT 1,
--    loginSuccess INTEGER,
--    loginFailed INTEGER,
--    accountExpiration TIMESTAMP NOT NULL,
--    accountLink VARCHAR(100)
--);
--
--CREATE TABLE IF NOT EXISTS udetails (
--    Id INTEGER PRIMARY KEY AUTO_INCREMENT,
--    userID INTEGER NOT NULL UNIQUE,
--    firstName VARCHAR(100),
--    lastName VARCHAR(100),
--    gender VARCHAR(100),
--    phone VARCHAR(100),
--    institution VARCHAR(100),
--    email VARCHAR(100),
--    address VARCHAR(100),
--    country VARCHAR(100),
--    privateUsage BOOLEAN,
--    foreign key (userID)
--    references korapusers (id)
--    on delete cascade
--);
--
--CREATE TABLE IF NOT EXISTS usettings (
--    Id INTEGER PRIMARY KEY AUTO_INCREMENT,
--    userID INTEGER NOT NULL UNIQUE,
--    fileNameForExport VARCHAR(100),
--    itemForSimpleAnnotation INTEGER,
--    leftContextItemForExport VARCHAR(100),
--    leftContextSizeForExport INTEGER,
--    locale VARCHAR(100),
--    leftContextItem VARCHAR(100),
--    leftContextSize INTEGER,
--    rightContextItem VARCHAR(100),
--    rightContextItemForExport VARCHAR(100),
--    rightContextSize INTEGER,
--    rightContextSizeForExport INTEGER,
--    selectedCollection VARCHAR(100),
--    queryLanguage VARCHAR(100),
--    pageLength INTEGER,
--    metadataQueryExpertModus BOOLEAN,
--    searchSettingsTab INTEGER,
--    selectedGraphType INTEGER,
--    selectedSortType VARCHAR(100),
--    selectedViewForSearchResults VARCHAR(100),
--    POSFoundry VARCHAR(100),
--    lemmaFoundry VARCHAR(100),
--    constFoundry VARCHAR(100),
--    relFoundry VARCHAR(100),
--    collectData BOOLEAN,
--    foreign key (userID)
--    references korapusers (id)
--    on delete cascade
--);
--
--CREATE OR REPLACE VIEW allusers AS
--    SELECT
--        id,
--        username,
--        password,
--        accountLock,
--        accountCreation,
--        type,
--        URI_PASS_Fragment,
--        URI_CONF_Fragment,
--        URI_Expiration,
--        loginSuccess,
--        loginFailed,
--        accountExpiration,
--        accountLink
--    from
--        korapusers
--    UNION ALL SELECT
--        id,
--        username,
--        NULL as password,
--        NULL as accountLock,
--        accountCreation,
--        type,
--        NULL as URI_PASS_Fragment,
--        NULL as URI_CONF_Fragment,
--        NULL as URI_Expiration,
--        loginSuccess,
--        loginFailed,
--        accountExpiration,
--        accountLink
--    from
--        shibusers;

---- why unsigned?
--CREATE TABLE IF NOT EXISTS r_store (
--id INTEGER PRIMARY KEY AUTO_INCREMENT,
--persistent_id VARCHAR(100) NOT NULL UNIQUE,
--name VARCHAR(100),
--description VARCHAR(300),
--parent_id Integer unsigned null,
--created timestamp default current_timestamp,
--type INTEGER NOT NULL,
--creator INTEGER NOT NULL
--);
--
--CREATE TABLE IF NOT EXISTS uqueries (
--    id INTEGER PRIMARY KEY,
--    queryLanguage VARCHAR(100),
--    name VARCHAR(100),
--    query VARCHAR(200),
--    description VARCHAR(150),
--    foreign key (id)
--    references r_store(id)
--    on delete cascade
--);

CREATE TABLE IF NOT EXISTS r_tree (
parent_id INTEGER,
child_id INTEGER,
depth INTEGER,
name_path VARCHAR(250),
PRIMARY KEY (parent_id , child_id),
foreign key (parent_id)
references r_store (id)
on delete cascade,
foreign key (child_id)
references r_store (id)
on delete cascade
);


CREATE TABLE IF NOT EXISTS cstorage (
    id INTEGER,
    refCorpus VARCHAR(100),
    query VARCHAR(500),
    -- is foreign key constraint valid after refactoring?
    foreign key (id) references r_store(id)
    on delete cascade);


CREATE TABLE IF NOT EXISTS matchInfo (id INTEGER PRIMARY KEY AUTO_INCREMENT, userid BIGINT NOT NULL,
matchInfo VARCHAR(100));

CREATE TABLE IF NOT EXISTS resourceRecords (
    AUD_ID INTEGER PRIMARY KEY AUTO_INCREMENT,
    AUD_RESOURCE VARCHAR(100),
    AUD_USER VARCHAR(100),
    AUD_LOC VARCHAR(100),
    AUD_OP VARCHAR(100),
    AUD_TIMESTAMP TIMESTAMP,
    AUD_FAILURE VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS databaseRecords (
    AUD_ID INTEGER PRIMARY KEY AUTO_INCREMENT,
    AUD_TARGET VARCHAR(100),
    AUD_USER VARCHAR(100),
    AUD_LOC VARCHAR(100),
    AUD_OP VARCHAR(100),
    AUD_TIMESTAMP TIMESTAMP,
    AUD_FAILURE VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS securityRecords (
    AUD_ID INTEGER PRIMARY KEY AUTO_INCREMENT,
    AUD_USER VARCHAR(100),
    AUD_LOC VARCHAR(100),
    AUD_OP VARCHAR(100),
    AUD_TIMESTAMP TIMESTAMP,
    AUD_FAILURE VARCHAR(100)
);


--CREATE TABLE IF NOT EXISTS doc_store (
--    id INTEGER PRIMARY KEY AUTO_INCREMENT,
--    persistent_id VARCHAR(100) UNIQUE,
--    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    disabled BOOLEAN default true
--);

-- last_modified timestamp ON UPDATE CURRENT_TIMESTAMP,
CREATE TABLE IF NOT EXISTS p_store (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    target_id BIGINT NOT NULL,
    created TIMESTAMP,
    creator INTEGER NOT NULL,
    posix SMALLINT NOT NULL,
    expire TIMESTAMP NULL,
    enable TIMESTAMP NULL,
    iprange VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS conditionDef (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    groupRef VARCHAR(100) NOT NULL,
    policyid INTEGER NOT NULL
);


CREATE TABLE IF NOT EXISTS groupStore (
    name VARCHAR(100) PRIMARY KEY,
    description VARCHAR(200),
    sym_use INTEGER DEFAULT -1,
    export VARCHAR(30) DEFAULT NULL,
    query_only VARCHAR(30) DEFAULT NULL,
    licence INTEGER DEFAULT -1,
    -- basically every resource we have is an academic resource, thus a non-commercial use is infered!
    commercial BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS groupUsers (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    userID BIGINT NOT NULL,
    groupRef VARCHAR(100) NOT NULL,
    admin BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (groupRef)
        REFERENCES groupStore (name) on delete cascade
);

CREATE TABLE IF NOT EXISTS paramStore (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    p_key VARCHAR(100) NOT NULL,
    p_value VARCHAR(150) NOT NULL,
    resource INTEGER DEFAULT -1,
    pid INTEGER DEFAULT -1,
    FOREIGN KEY (resource)
        REFERENCES r_store(id)
    on delete cascade,
    FOREIGN KEY (pid)
        REFERENCES p_store(id)
    on delete cascade
);

CREATE TABLE IF NOT EXISTS paramMapping (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    policyID INTEGER NOT NULL,
    paramID INTEGER NOT NULL,
    value VARCHAR(100) NOT NULL,
    flag BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (paramID)
        REFERENCES paramStore (id),
    FOREIGN KEY (policyID)
        REFERENCES p_store (id)
);

create or replace view p_view as
select
    po.id as pid,
    po.target_id as id,
    rs.persistent_id as persistent_id,
    rs.name as name,
    rs.type as type,
    c.groupref as groupref,
    po.posix as perm,
    po.creator as creator,
    po.expire as expire,
    po.enable as enable,
    po.iprange as iprange
from
p_store as po
inner join
conditionDef as c ON c.policyid = po.id
inner join
r_store as rs ON rs.id = po.target_id
union all select
              - 1 as pid,
              rs.id as id,
              rs.persistent_id as persistent_id,
              rs.name as name,
              type as type,
              'self' as groupref,
              127 as perm,
              creator,
              NULL as expire,
              rs.created as enable,
              null as iprange
          from
          r_store as rs;


-- indices
create trigger delete_policy after delete on r_store
for each row delete from p_store where target_id=OLD.id;

DELIMITER //
CREATE TRIGGER tree_entry_insert AFTER INSERT ON r_store FOR EACH ROW BEGIN
	INSERT INTO r_tree (parent_id, child_id, depth, name_path)
	VALUES (NEW.id, NEW.id, 0, NEW.name);
	INSERT INTO r_tree (parent_id, child_id, depth, name_path)
	SELECT parent_id, NEW.id, rt.depth + 1, concat(name_path,"/",NEW.name) FROM r_tree WHERE child_id = NEW.parent_id;
END; //

DELIMITER ;

-- todo: are this automatically adapted when refactoring?
CREATE INDEX group_index ON groupUsers(userid);
CREATE INDEX policy_index ON conditionDef(policyid);
CREATE UNIQUE INDEX r_tree_index ON r_tree (parent_id, depth, child_id);
CREATE UNIQUE INDEX para_unique ON paramStore (p_key, p_value);

-- foreign key constraints






