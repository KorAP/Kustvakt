package de.ids_mannheim.korap.web.input;


import com.fasterxml.jackson.annotation.JsonProperty;

import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.service.QueryService;
import de.ids_mannheim.korap.web.controller.QueryReferenceController;
import de.ids_mannheim.korap.web.controller.VirtualCorpusController;
import lombok.Getter;
import lombok.Setter;

/** Java POJO of JSON input of the virtual corpus and query controllers 
 * for creating and editing virtual corpora and query references.
 * 
 * @author margaretha
 * @see VirtualCorpusController
 * @see QueryReferenceController
 * @see QueryService
 */
@Getter
@Setter
public class QueryJson {
    // default false
    @JsonProperty("is_cached")
    private boolean isCached;
    
    // required
    private ResourceType type;
    // required for queryType="VIRTUAL_CORPUS"
    @JsonProperty("corpus_query")
    private String corpusQuery;
    // required for queryType="QUERY"
    private String query;
    @JsonProperty("query_language")
    private String queryLanguage;
    
    // optional
    private String definition;
    private String description;
    private String status;
    @JsonProperty("query_version")
    private String queryVersion;
    @JsonProperty("query_type")
    private QueryType queryType;
}