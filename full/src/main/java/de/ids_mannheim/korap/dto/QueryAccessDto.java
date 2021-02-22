package de.ids_mannheim.korap.dto;

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
    private int accessId;
    private String createdBy;
    private int queryId;
    private String queryName;
    private int userGroupId;
    private String userGroupName;

    @Override
    public String toString () {
        return "accessId=" + accessId + ", createdBy=" + createdBy + " , queryId="
                + queryId + ", queryName=" + queryName + ", userGroupId=" + userGroupId
                + ", userGroupName=" + userGroupName;
    }
}
