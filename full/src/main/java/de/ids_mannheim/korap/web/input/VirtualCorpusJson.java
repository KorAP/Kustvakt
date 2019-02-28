package de.ids_mannheim.korap.web.input;


import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.service.VirtualCorpusService;
import de.ids_mannheim.korap.web.controller.VirtualCorpusController;
import lombok.Getter;
import lombok.Setter;

/** Java POJO of JSON input of the virtual corpus controller for 
 * creating and editing virtual corpora.
 * 
 * @author margaretha
 * @see VirtualCorpusController
 * @see VirtualCorpusService
 */
@Getter
@Setter
public class VirtualCorpusJson {
    // default false
    private boolean isCached;
    
    // required in creating VCs
    private VirtualCorpusType type;
    private String corpusQuery;
    
    // optional
    private String definition;
    private String description;
    private String status;
}