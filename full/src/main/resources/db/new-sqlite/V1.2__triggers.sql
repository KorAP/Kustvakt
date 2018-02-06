--CREATE TRIGGER insert_member_status AFTER INSERT ON user_group_member
--     BEGIN
--      UPDATE user_group_member SET status_date = DATETIME('NOW', 'utc')  WHERE rowid = new.rowid;
--     END;
--
CREATE TRIGGER update_member_status AFTER UPDATE ON user_group_member	
     BEGIN
      UPDATE user_group_member SET status_date = (datetime('now','localtime'))  WHERE rowid = old.rowid;
     END;   
