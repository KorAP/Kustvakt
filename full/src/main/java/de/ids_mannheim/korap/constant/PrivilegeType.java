package de.ids_mannheim.korap.constant;

import de.ids_mannheim.korap.entity.Privilege;
import de.ids_mannheim.korap.entity.Role;

/** Defines the privilege or permissions of users or admins 
 * based on their roles.
 * 
 * @author margaretha
 * @see Privilege
 * @see Role
 */
public enum PrivilegeType {
    READ, WRITE, DELETE;
}
