package de.ids_mannheim.korap.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import de.ids_mannheim.korap.constants.VirtualCorpusType;
import lombok.Getter;
import lombok.Setter;

/** Describes virtual corpora.
 * 
 *  Any user may create a virtual corpus and share it to a user group.
 *  However, if the user is not a user-group admin, the virtual corpus 
 *  will not be shared until a user-group admin accept his/her request.
 *   
 * @author margaretha
 *
 * @see VirtualCorpusAccessGroup
 * @see UserGroup
 */
@Setter
@Getter
@Entity
@Table(name = "virtual_corpus")
public class VirtualCorpus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    @Enumerated(EnumType.STRING)
    private VirtualCorpusType type;
    private String status;
    private String description;
    @Column(name = "required_access")
    private String requiredAccess;
    @Column(name = "collection_query")
    private String collectionQuery;
    private String definition;
    @Column(name = "created_by")
    private String createdBy;

    @OneToMany(mappedBy = "userGroup", fetch=FetchType.LAZY)
    List<VirtualCorpusAccessGroup> accessGroup;


    @Override
    public String toString () {
        return "id=" + id + ", name= " + name + ", type= " + type + ", status= "
                + status + ", description=" + description + ", requiredAccess="
                + requiredAccess + ", collectionQuery= " + collectionQuery
                + ", definition= " + definition + ", createdBy= " + createdBy;
    }
}
