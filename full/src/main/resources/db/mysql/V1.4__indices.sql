-- todo: are this automatically adapted when refactoring?
CREATE INDEX group_index ON group_users(user_id, group_id);
CREATE INDEX policy_index ON group_ref(policy_id);
CREATE UNIQUE INDEX resource_tree_index ON resource_tree (parent_id, depth, child_id);
CREATE UNIQUE INDEX param_unique ON param_store (p_key, p_value);
