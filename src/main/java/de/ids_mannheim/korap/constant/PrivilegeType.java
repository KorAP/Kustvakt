package de.ids_mannheim.korap.constant;

/**
 * Defines the privilege or permissions of users or admins
 * based on their roles.
 * 
 * @author margaretha
 * @see Role
 */
public enum PrivilegeType {
    READ_MEMBER, 
    WRITE_MEMBER, 
    DELETE_MEMBER, 
    SHARE_QUERY,
    DELETE_QUERY,
    READ_QUERY,
    READ_LARGE_SNIPPET;
}
