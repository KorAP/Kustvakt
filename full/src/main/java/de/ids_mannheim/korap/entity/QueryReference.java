package de.ids_mannheim.korap.entity;

import java.util.List;

import javax.persistence.CascadeType;
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

import de.ids_mannheim.korap.constant.VirtualCorpusType;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes the query reference table.
 *
 * It is yet not as complete as the Virtual Corpus implementation,
 * as it has no mechanism for sharing any query references.
 * 
 * @author diewald
 *
 * @see VirtualCorpus
 */
@Setter
@Getter
@Entity
@Table(name = "query_reference")
public class QueryReference implements Comparable<QueryReference> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    @Enumerated(EnumType.STRING)
    private VirtualCorpusType type; // TODO (nd): This should be named RessourceType
    private String status;
    private String description;
    // @Enumerated(EnumType.STRING)
    @Column(name = "required_access")
    private String requiredAccess;
    //private CorpusAccess requiredAccess;
    @Column(name = "query")
    private String koralQuery;
    private String definition;
    @Column(name = "created_by")
    private String createdBy;

    /*
    @OneToMany(mappedBy = "queryReference", fetch = FetchType.LAZY,
            cascade = CascadeType.REMOVE)
    private List<VirtualCorpusAccess> virtualCorpusAccess;
    */

    @Override
    public String toString () {
        return "id=" + id +
            ", name= " + name +
            ", type= " + type +
            ", status= " + status +
            ", description=" + description +
            // ", requiredAccess=" + requiredAccess +
            ", query= " + koralQuery +
            ", definition= " + definition +
            ", createdBy= " + createdBy;
    }

    @Override
    public int hashCode () {
        int prime = 37;
        int result = 1;
        result = prime * result + id;
        result = prime * result + name.hashCode();
        result = prime * result + createdBy.hashCode();
        return result;
    }

    @Override
    public boolean equals (Object obj) {
        QueryReference q = (QueryReference) obj;
        return (this.id == q.getId()) ? true : false;
    }

    @Override
    public int compareTo (QueryReference o) {
        if (this.getId() > o.getId()) {
            return 1;
        }
        else if (this.getId() < o.getId()) {
            return -1;
        }
        return 0;
    }
}
