package de.ids_mannheim.korap.constant;

import de.ids_mannheim.korap.entity.UserGroup;

/**
 * Defines possible statuses of {@link UserGroup}s
 * 
 * @author margaretha
 *
 */
public enum UserGroupStatus {
    ACTIVE, 
    // group members cannot see the group
    HIDDEN;
}
