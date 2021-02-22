-- member roles

-- marlin group
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="marlin" AND group_id=1),
	(SELECT id FROM role WHERE name = "USER_GROUP_ADMIN");
	
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="marlin" AND group_id=1),
	(SELECT id FROM role WHERE name = "QUERY_ACCESS_ADMIN");
	
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="dory" AND group_id=1),
	(SELECT id FROM role WHERE name = "USER_GROUP_ADMIN");
	
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="dory" AND group_id=1),
	(SELECT id FROM role WHERE name = "QUERY_ACCESS_ADMIN");
	
	
-- dory group
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="dory" AND group_id=2),
	(SELECT id FROM role WHERE name = "USER_GROUP_ADMIN");
	
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="dory" AND group_id=2),
	(SELECT id FROM role WHERE name = "QUERY_ACCESS_ADMIN");
	
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="nemo" AND group_id=2),
	(SELECT id FROM role WHERE name = "USER_GROUP_MEMBER");
	
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="nemo" AND group_id=2),
	(SELECT id FROM role WHERE name = "QUERY_ACCESS_MEMBER");


-- auto group
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="pearl" AND group_id=3),
	(SELECT id FROM role WHERE name = "QUERY_ACCESS_MEMBER");

