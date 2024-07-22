package de.ids_mannheim.korap.dto;

import java.util.List;

import de.ids_mannheim.korap.entity.UserGroupMember;
import lombok.Getter;
import lombok.Setter;

/**
 * Defines the structure of query accesses, e.g. as JSON
 * objects in HTTP Responses.
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
public class QueryAccessDto {
    private int roleId;
    private int queryId;
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
