package de.ids_mannheim.korap.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

/**
 * Defines the structure of query roles, e.g. as JSON
 * objects in HTTP Responses.
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
public class RoleDto {
    private int roleId;
    private String privilege;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int queryId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String queryName;
    private int userGroupId;
    private String userGroupName;
    private List<String> members;

    @Override
    public String toString () {
        return "roleId=" + roleId + " , queryId=" + queryId + ", queryName="
                + queryName + ", userGroupId=" + userGroupId
                + ", userGroupName=" + userGroupName 
                +", members=" + members;
    }
}
