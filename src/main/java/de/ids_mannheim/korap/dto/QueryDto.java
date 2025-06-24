package de.ids_mannheim.korap.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.entity.QueryDO;
import lombok.Getter;
import lombok.Setter;

/**
 * Defines the structure of {@link QueryDO} description to be
 * sent as JSON objects in HTTP responses.
 * 
 * @author margaretha
 *
 */
@Getter
@Setter
@JsonInclude(Include.NON_DEFAULT)
public class QueryDto {

    private int id;
    private String name;
    private String type;
    private String status;
    private String description;
    private String requiredAccess;
    private String createdBy;

    private long numberOfDoc;
    private long numberOfParagraphs;
    private long numberOfSentences;
    private long numberOfTokens;

    private String query;
    private String queryLanguage;
    private JsonNode koralQuery;
}
