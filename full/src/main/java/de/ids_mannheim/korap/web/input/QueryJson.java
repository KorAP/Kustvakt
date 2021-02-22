package de.ids_mannheim.korap.web.input;


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
    private boolean isCached;
    
    // required
    private ResourceType type;
    // required for queryType="VIRTUAL_CORPUS"
    private String corpusQuery;
    // required for queryType="QUERY"
    private String query;
    private String queryLanguage;
    
    // optional
    private String definition;
    private String description;
    private String status;
    private String queryVersion;
    private QueryType queryType;
}