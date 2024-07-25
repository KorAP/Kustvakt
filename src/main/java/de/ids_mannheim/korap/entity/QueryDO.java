package de.ids_mannheim.korap.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes the query table and its relation to {@link QueryAccess}.
 * 
 * Any user may create a query and share it to a user group.
 * However, if the user is not a user-group admin, the query
 * will not be shared until a user-group admin accept his/her request.
 * 
 * @author margaretha
 *
 * @see QueryAccess
 * @see UserGroup
 */
@Setter
@Getter
@Entity
@Table(name = "query")
public class QueryDO implements Comparable<QueryDO> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    @Enumerated(EnumType.STRING)
    private ResourceType type;
    private String status;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "required_access")
    private CorpusAccess requiredAccess;
    @Column(name = "koral_query")
    private String koralQuery;
    private String definition;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "is_cached")
    private boolean isCached;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_type")
    private QueryType queryType;
    private String query;
    @Column(name = "query_language")
    private String queryLanguage;

    @OneToMany(mappedBy = "query", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Role> roles;

    @Override
    public String toString () {
        return "id=" + id + ", name= " + name + ", type= " + type + ", status= "
                + status + ", description=" + description + ", requiredAccess="
                + requiredAccess + ", koralQuery= " + koralQuery
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
        QueryDO query = (QueryDO) obj;
        return (this.id == query.getId()) ? true : false;
    }

    @Override
    public int compareTo (QueryDO o) {
        if (this.getId() > o.getId()) {
            return 1;
        }
        else if (this.getId() < o.getId()) {
            return -1;
        }
        return 0;
    }
}
