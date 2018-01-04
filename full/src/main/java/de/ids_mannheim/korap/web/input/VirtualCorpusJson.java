package de.ids_mannheim.korap.web.input;


import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.VirtualCorpusService;
import de.ids_mannheim.korap.utils.ParameterChecker;
import de.ids_mannheim.korap.web.controller.VirtualCorpusController;
import lombok.Getter;
import lombok.Setter;

/** Java POJO of JSON input of the virtual corpus service for 
 * creating and editing virtual corpora.
 * 
 * @author margaretha
 * @see VirtualCorpusController
 * @see VirtualCorpusService
 */
@Getter
@Setter
public class VirtualCorpusJson {

    // required in creating VCs
    private String name;
    private VirtualCorpusType type;
    private String collectionQuery;
    private String createdBy;
    
    // required in editing VCs
    private int id;
    
    // optional
    private String definition;
    private String description;
    private String status;


    public void setCollectionQuery (String collectionQuery)
            throws KustvaktException {
        
        this.collectionQuery = collectionQuery;
    }
}