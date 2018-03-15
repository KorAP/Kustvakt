package de.ids_mannheim.korap.dto;

import java.util.List;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import lombok.Getter;
import lombok.Setter;

/** Defines UserGroupMember description, e.g. to be sent as 
 *  JSON objects in HTTP Responses.
 * @author margaretha
 *
 */
@Setter
@Getter
public class UserGroupMemberDto {
    private String userId;
    private GroupMemberStatus status;
    private List<String> roles;
}
