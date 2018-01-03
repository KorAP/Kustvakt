package de.ids_mannheim.korap.web.input;


import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.VirtualCorpusService;
import de.ids_mannheim.korap.utils.ParameterChecker;
import de.ids_mannheim.korap.web.controller.VirtualCorpusController;
import lombok.Getter;
import lombok.Setter;

/** Java POJO of JSON input of the virtual corpus service for 
 * creating virtual corpora.
 * 
 * @author margaretha
 * @see VirtualCorpusController#createVC(javax.ws.rs.core.SecurityContext, VirtualCorpusJson)
 * @see VirtualCorpusService#createVC(VirtualCorpusJson, String)
 */
@Getter
@Setter
public class VirtualCorpusJson {

    // required
    private String name;
    private VirtualCorpusType type;
    private String createdBy;
    private String collectionQuery;

    // optional
    private String definition;
    private String description;
    private String status;

    public void setType (VirtualCorpusType type) throws KustvaktException {
        ParameterChecker.checkObjectValue(type, "VirtualCorpusType");
        this.type = type;
    }

    public void setName (String name) throws KustvaktException {
        ParameterChecker.checkStringValue(name, "name");
        this.name = name;
    }

    public void setCreatedBy (String createdBy) throws KustvaktException {
        ParameterChecker.checkStringValue(createdBy, "createdBy");
        this.createdBy = createdBy;
    }

    public void setCollectionQuery (String collectionQuery)
            throws KustvaktException {
        ParameterChecker.checkStringValue(collectionQuery, "collectionQuery");
        this.collectionQuery = collectionQuery;
    }
}