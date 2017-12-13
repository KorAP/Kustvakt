package de.ids_mannheim.korap.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VirtualCorpusDto {

    private int id;
    private String name;
    private String type;
    private String status;
    private String description;
    private String access;
    private String createdBy;
    
    private int numberOfDoc;
}
