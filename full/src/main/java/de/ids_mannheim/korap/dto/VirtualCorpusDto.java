package de.ids_mannheim.korap.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

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
@JsonInclude(Include.NON_DEFAULT)
public class VirtualCorpusDto {

    private int id;
    private String name;
    private String type;
    private String status;
    private String description;
    private String requiredAccess;
    private String createdBy;
    
    private int numberOfDoc;
    private int numberOfParagraphs;
    private int numberOfSentences;
    private int numberOfTokens;
    
    private JsonNode koralQuery;
}
