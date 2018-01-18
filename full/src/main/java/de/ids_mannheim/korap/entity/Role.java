package de.ids_mannheim.korap.entity;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(unique = true)
    private String name;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private List<UserGroupMember> userGroupMembers;

    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER,
            cascade = CascadeType.REMOVE)
    private List<Privilege> privileges;

    public String toString () {
        return "id=" + id + ", name=" + name;
    }
}
