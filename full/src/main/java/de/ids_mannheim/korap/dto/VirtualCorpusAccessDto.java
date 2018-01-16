package de.ids_mannheim.korap.dto;

import lombok.Getter;
import lombok.Setter;

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
