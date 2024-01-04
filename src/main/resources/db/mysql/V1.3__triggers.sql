delimiter |

CREATE TRIGGER delete_member AFTER UPDATE ON user_group
	FOR EACH ROW 
	BEGIN
		UPDATE user_group_member 
		SET status = "DELETED"
		WHERE NEW.status = "DELETED" 
			AND  OLD.status != "DELETED" 
			AND group_id = NEW.id;
	END;
|
	
delimiter ;