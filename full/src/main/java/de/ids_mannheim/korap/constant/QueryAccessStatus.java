package de.ids_mannheim.korap.constant;

import de.ids_mannheim.korap.entity.QueryAccess;

/** Defines possible statuses of {@link QueryAccess}
 * 
 * @author margaretha
 * @see QueryAccess
 *
 */
public enum QueryAccessStatus {

    ACTIVE, DELETED,
    // has not been used yet
    PENDING,
    // access for hidden group
    // maybe not necessary?
    HIDDEN;
}
