package de.ids_mannheim.korap.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Defines the structure of virtual corpus accesses, e.g. as JSON
 * objects in HTTP Responses.
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
public class VirtualCorpusAccessDto {
    private int accessId;
    private String createdBy;
    private int vcId;
    private String vcName;
    private int userGroupId;
    private String userGroupName;
}
