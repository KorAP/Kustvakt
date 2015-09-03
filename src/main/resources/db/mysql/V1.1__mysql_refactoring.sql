-- alter table korapusers TO korap_users;
rename table usettings TO user_settings;
alter table user_settings drop column itemForSimpleAnnotation;
alter table user_settings drop column searchSettingsTab;
alter table user_settings drop column selectedGraphType;
alter table user_settings drop column selectedSortType;
alter table user_settings drop column selectedViewForSearchResults;
rename table udetails to user_details;
rename table uqueries to user_queries;
rename table korapusers to korap_users;
rename table shibusers to shib_users;
rename table matchInfo to match_info;

alter table korap_users change column URI_PASS_Fragment uri_fragment VARCHAR(100);
alter table korap_users change column URI_Expiration uri_expiration TIMESTAMP;
drop view allusers;

rename table r_store TO resource_store;
rename table r_tree TO resource_tree;

rename table groupStore TO group_store;
rename table groupUsers TO group_users;
rename table paramStore TO param_store;
rename table paramMapping TO param_map;

-- todo: what about the moving of the entries?
-- rather rename than drop!
-- drop table cstorage;
-- todo: test
rename table cstorage to coll_store;
alter table coll_store add column (
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
userID INTEGER);
alter table coll_store drop column refCorpus;


-- do not recreate -- maintain data
--CREATE TABLE IF NOT EXISTS coll_store (
--id INTEGER PRIMARY KEY AUTO_INCREMENT,
--persistentID VARCHAR(150) UNIQUE,
--name VARCHAR(150),
--description VARCHAR(200),
--query VARCHAR(500),
--created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--userID INTEGER,
--foreign key(userID)
--references korap_users(id)
--on delete cascade
--);


drop table doc_trace;

CREATE TABLE IF NOT EXISTS doc_store (
id VARCHAR(230) PRIMARY KEY,
persistent_id VARCHAR(230) UNIQUE,
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
disabled BOOLEAN default true
);



rename table p_store to policy_store;
rename table conditionDef to group_ref;
alter table group_ref change groupRef group_id VARCHAR(100) NOT NULL;
alter table group_ref change policyId policy_id VARCHAR(100) NOT NULL;

drop view p_view;
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
              'self' as groupId,
              127 as perm,
              creator,
              NULL as expire,
              rs.created as enable,
              null as iprange
          from
          resource_store as rs;


drop table resourceRecords;
drop table databaseRecords;
drop table securityRecords;

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



drop trigger tree_entry_insert;

DELIMITER //
CREATE TRIGGER tree_entry_insert AFTER INSERT ON resource_store FOR EACH ROW BEGIN
	INSERT INTO resource_tree (parent_id, child_id, depth, name_path)
	VALUES (NEW.id, NEW.id, 0, NEW.name);
	INSERT INTO resource_tree (parent_id, child_id, depth, name_path)
	SELECT parent_id, NEW.id, depth + 1, concat(name_path,"/",NEW.name)
	FROM resource_tree WHERE child_id = NEW.parent_id;
END; //

DELIMITER ;


drop trigger delete_policy;

create trigger delete_policy after delete on resource_store
for each row delete from policy_store where target_id=OLD.id;