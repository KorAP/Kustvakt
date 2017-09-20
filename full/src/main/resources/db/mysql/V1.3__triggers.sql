-- indices
DELIMITER $$
create trigger delete_policy after delete on resource_store
for each row begin
    delete from policy_store where target_id=OLD.id;
end; $$

CREATE TRIGGER tree_entry_insert AFTER INSERT ON resource_store FOR EACH ROW BEGIN
	INSERT INTO resource_tree (parent_id, child_id, depth, name_path)
	VALUES (NEW.id, NEW.id, 0, NEW.name);
	INSERT INTO resource_tree (parent_id, child_id, depth, name_path)
	SELECT parent_id, NEW.id, depth + 1, concat(name_path,"/",NEW.name) FROM resource_tree WHERE child_id = NEW.parent_id;
END; $$

DELIMITER ;