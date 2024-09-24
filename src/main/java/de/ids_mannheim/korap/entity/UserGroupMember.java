package de.ids_mannheim.korap.entity;

import java.util.Set;

import de.ids_mannheim.korap.constant.PredefinedRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Describes members of user groups. Only member of predefined role
 * group admin can see the rest of members.
 * 
 * @author margaretha
 * @see UserGroup
 * @see Role
 * @see PredefinedRole
 */
@Setter
@Getter
@Entity
@Table(name = "user_group_member", indexes = {
        @Index(unique = true, columnList = "user_id, group_id") })
public class UserGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "user_id")
    private String userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    private UserGroup group;

    /**
     * Information about roles is deemed to be always necessary to
     * describe a member.
     * 
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "group_member_role", joinColumns = @JoinColumn(name = "group_member_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), uniqueConstraints = @UniqueConstraint(columnNames = {
            "group_member_id", "role_id" }))
    private Set<Role> roles;

    @Override
    public String toString () {
        return "id=" + id + ", group= " + group + ", userId= " + userId
                + ", roles=" + roles;
    }
}
