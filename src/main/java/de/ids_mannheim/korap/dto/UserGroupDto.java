package de.ids_mannheim.korap.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Defines the structure of UserGroup description, e.g.
 * to be sent as JSON objects in HTTP response.
 * 
 * @author margaretha
 *
 */
@Setter
@Getter
public class UserGroupDto {

    private int id;
    private String name;
    private String description;
    private String owner;
    private UserGroupStatus status;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<UserGroupMemberDto> members;
}
