package de.ids_mannheim.korap.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/** Describes user roles for example in managing a group or 
 *  virtual corpora of a group.
 *  
 * @author margaretha
 * @see Privilege
 */
@Setter
@Getter
@Entity
@Table(name = "role")
public class Role {
    @Id
    @Column(unique = true)
    private String id;
    private String privilege;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private List<UserGroupMember> userGroupMembers;

    public String toString () {
        return "id=" + id + ", privilege= " + privilege;
    }
}
