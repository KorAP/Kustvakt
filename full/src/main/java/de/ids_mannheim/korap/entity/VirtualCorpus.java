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

import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes the virtual corpora table and its relation to
 * VirtualCorpusAccess.
 * 
 * Any user may create a virtual corpus and share it to a user group.
 * However, if the user is not a user-group admin, the virtual corpus
 * will not be shared until a user-group admin accept his/her request.
 * 
 * @author margaretha
 *
 * @see VirtualCorpusAccess
 * @see UserGroup
 */
@Setter
@Getter
@Entity
@Table(name = "virtual_corpus")
public class VirtualCorpus implements Comparable<VirtualCorpus> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    @Enumerated(EnumType.STRING)
    private ResourceType type;
    @Enumerated(EnumType.STRING)
    @Column(name = "query_type")
    private QueryType queryType;
    private String status;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "required_access")
    private CorpusAccess requiredAccess;
    @Column(name = "corpus_query")
    private String koralQuery;
    private String definition;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "is_cached")
    private boolean isCached;

    @OneToMany(mappedBy = "virtualCorpus", fetch = FetchType.LAZY,
            cascade = CascadeType.REMOVE)
    private List<VirtualCorpusAccess> virtualCorpusAccess;

    @Override
    public String toString () {
        return "id=" + id + ", name= " + name + ", type= " + type + ", status= "
                + status + ", description=" + description + ", requiredAccess="
                + requiredAccess + ", corpusQuery= " + koralQuery
                + ", definition= " + definition + ", createdBy= " + createdBy;
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
        VirtualCorpus vc = (VirtualCorpus) obj;
        return (this.id == vc.getId()) ? true : false;
    }

    @Override
    public int compareTo (VirtualCorpus o) {
        if (this.getId() > o.getId()) {
            return 1;
        }
        else if (this.getId() < o.getId()) {
            return -1;
        }
        return 0;
    }
}
