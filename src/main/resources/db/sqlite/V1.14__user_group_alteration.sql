-- please commented out the indexes in V1.1 later
DROP INDEX IF EXISTS group_member_role_index;
DROP INDEX IF EXISTS user_group_member_status_index;

-- please commented out the triggers in V1.2__triggers.sql later
DROP TRIGGER IF EXISTS insert_member_status;
DROP TRIGGER IF EXISTS update_member_status;
DROP TRIGGER IF EXISTS delete_member;

ALTER TABLE user_group
ADD COLUMN created_date TIMESTAMP;

ALTER TABLE user_group
DROP COLUMN deleted_by;

ALTER TABLE user_group_member
DROP COLUMN created_by;

ALTER TABLE user_group_member
DROP COLUMN deleted_by;

ALTER TABLE user_group_member
DROP COLUMN status;

ALTER TABLE user_group_member
DROP COLUMN status_date;
  