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

import de.ids_mannheim.korap.constant.UserGroupStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes user group table and its relations to UserGroupMember and
 * {@link QueryAccess}.
 * 
 * Any user may create a user group and send invitations to group
 * member by username. Any group member may reject the invitation
 * or unsubscribe from the group.
 * 
 * @author margaretha
 * @see UserGroupMember
 * @see QueryAccess
 */
@Setter
@Getter
@Entity
@Table(name = "user_group")
public class UserGroup implements Comparable<UserGroup> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    // unique
    private String name;
    private String description;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "deleted_by")
    private String deletedBy;

    @Enumerated(EnumType.STRING)
    private UserGroupStatus status;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY,
            cascade = CascadeType.REMOVE)
    private List<UserGroupMember> members;

    @OneToMany(mappedBy = "userGroup", fetch = FetchType.LAZY,
            cascade = CascadeType.REMOVE)
    private List<QueryAccess> queryAccess;

    @Override
    public String toString () {
        return "id=" + id + ", name= " + name + ", createdBy= " + createdBy;
    }

    @Override
    public int compareTo (UserGroup o) {
        if (this.getId() > o.getId()) {
            return 1;
        }
        else if (this.getId() < o.getId()) {
            return -1;
        }
        return 0;
    }
}
