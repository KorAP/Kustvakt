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
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import lombok.Getter;
import lombok.Setter;

/** Describes members of user groups. Only member of predefined role 
 *  group admin can see the rest of members.
 * 
 *  @author margaretha
 *  @see UserGroup
 *  @see Role
 *  @see PredefinedRole
 */
@Setter
@Getter
@Entity
@Table(name = "user_group_member",
        indexes = { @Index(unique = true, columnList = "user_id, group_id") })
public class UserGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "deleted_by")
    private String deletedBy;

    @Enumerated(EnumType.STRING)
    private GroupMemberStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private UserGroup group;

    /** Information about roles is deemed always necessary to describe a member.
     * 
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "group_member_role",
            joinColumns = @JoinColumn(name = "group_member_id",
                    referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id",
                    referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = { "group_member_id", "role_id" }))
    private List<Role> roles;

    @Override
    public String toString () {
        return "id=" + id + ", group= " + group + ", userId= " + userId
                + ", createdBy= " + createdBy + ", deletedBy= " + deletedBy
                + ", roles=" + roles;
    }
}
