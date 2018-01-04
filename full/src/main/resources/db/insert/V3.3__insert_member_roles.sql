-- member roles
-- dory group
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="dory" AND group_id=2),
	(SELECT id FROM role WHERE name = "group admin");
	
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="dory" AND group_id=2),
	(SELECT id FROM role WHERE name = "vc admin");
	
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="nemo" AND group_id=2),
	(SELECT id FROM role WHERE name = "group member");
	
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="nemo" AND group_id=2),
	(SELECT id FROM role WHERE name = "vc member");

-- auto group
INSERT INTO group_member_role(group_member_id,role_id)
SELECT
	(SELECT id FROM user_group_member WHERE user_id="pearl" AND group_id=3),
	(SELECT id FROM role WHERE name = "vc member");

