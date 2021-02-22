package de.ids_mannheim.korap.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.ids_mannheim.korap.constant.QueryAccessStatus;
import lombok.Getter;
import lombok.Setter;

/** Describes the relationship between virtual corpora and user groups, 
 *  i.e. which groups may access which virtual corpora, and the history 
 *  of group-access management.  
 * 
 * @author margaretha
 * @see QueryDO
 * @see UserGroup
 */
@Setter
@Getter
@Entity
@Table(name = "query_access")
public class QueryAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "approved_by")
    private String approvedBy;
    @Column(name = "deleted_by")
    private String deletedBy;

    @Enumerated(EnumType.STRING)
    private QueryAccessStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "query_id", referencedColumnName = "id")
    private QueryDO query;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_group_id", referencedColumnName = "id")
    private UserGroup userGroup;


    @Override
    public String toString () {
        return "id=" + id + ", query= " + query
                + ", userGroup= " + userGroup;
    }
}
