package de.ids_mannheim.korap.dto;

import java.util.List;
import java.util.Set;

import de.ids_mannheim.korap.constant.PrivilegeType;
import lombok.Getter;
import lombok.Setter;

/**
 * Defines UserGroupMember description, e.g. to be sent as
 * JSON objects in HTTP Responses.
 * 
 * @author margaretha
 *
 */
@Setter
@Getter
public class UserGroupMemberDto {
    private String userId;
    private Set<String> roles;
    private List<PrivilegeType> privileges;
}
