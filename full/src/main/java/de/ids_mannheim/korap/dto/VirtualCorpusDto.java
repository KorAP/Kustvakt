package de.ids_mannheim.korap.dto;

import de.ids_mannheim.korap.entity.VirtualCorpus;
import lombok.Getter;
import lombok.Setter;

/** Defines the structure of {@link VirtualCorpus} description to be 
 *  sent as JSON objects in HTTP responses. 
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
public class VirtualCorpusDto {

    private int id;
    private String name;
    private String type;
    private String status;
    private String description;
    private String requiredAccess;
    private String createdBy;
    
    private int numberOfDoc;
    private String koralQuery;}
