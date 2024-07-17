package de.ids_mannheim.korap.entity;

import java.util.List;

import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.PrivilegeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes user roles for example in managing a group or
 * virtual corpora of a group.
 * 
 * @author margaretha
 * @see Privilege
 */
@Setter
@Getter
@Entity
@Table(name = "role")
public class Role implements Comparable<Role> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(unique = true)
    @Enumerated(EnumType.STRING)
    private PredefinedRole name;
    @Enumerated(EnumType.STRING)
    private PrivilegeType privilege;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", referencedColumnName = "id")
    private QueryDO query;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private UserGroup userGroup;
    
//    @ManyToMany(fetch = FetchType.LAZY)
//    @JoinTable(
//        name = "role_user_roles",
//        joinColumns = @JoinColumn(name = "role_id"),
//        inverseJoinColumns = @JoinColumn(name = "user_role_id")
//    )
//    private Set<UserRole> user_roles;
    
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private List<UserGroupMember> userGroupMembers;
//
//    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
//    private List<Privilege> privileges;
    
    public Role () {}
    
    public Role (PredefinedRole name, PrivilegeType privilege, UserGroup group) {
        setName(name);
        setPrivilege(privilege);
        setUserGroup(group);
    }

    public String toString () {
        return "id=" + id + ", name=" + name;
    }

    @Override
    public int compareTo (Role o) {
        if (this.getId() > o.getId()) {
            return 1;
        }
        else if (this.getId() < o.getId()) {
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals (Object obj) {
        Role r = (Role) obj;
        if (this.id == r.getId() && this.name.equals(r.getName())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode () {
        int hash = 7;
        hash = 31 * hash + (int) id;
        hash = 31 * hash + (name == null ? 0 : name.hashCode());
        return hash;
    }
}
