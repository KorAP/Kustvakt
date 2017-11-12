package de.ids_mannheim.korap.web.input;

import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.ParameterChecker;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VirtualCorpusFromJson {

    private String name;
    private VirtualCorpusType type;
    private String createdBy;
    private String collectionQuery;

    // optional
    private String definition;
    private String description;
    private String status;

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