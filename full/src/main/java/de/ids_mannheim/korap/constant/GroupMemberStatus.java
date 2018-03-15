package de.ids_mannheim.korap.constant;

/** Defines the status of a user-group member
 * 
 * @author margaretha
 *
 */
public enum GroupMemberStatus {
    ACTIVE, 
    // membership invitation was sent and has not been accepted 
    // or rejected yet
    PENDING, 
    // either membership invitation was rejected or the member was 
    // deleted by a user-group admin
    DELETED;
}
