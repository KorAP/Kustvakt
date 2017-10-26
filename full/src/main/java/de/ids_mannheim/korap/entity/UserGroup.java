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
import javax.persistence.OneToMany;
import javax.persistence.Table;

import de.ids_mannheim.korap.constant.UserGroupStatus;
import lombok.Getter;
import lombok.Setter;

/** Describes user groups.
 * 
 * Any user may create a user group and send invitations to group 
 * member by username. Any group member may reject the invitation
 * or unsubscribe from the group.
 * 
 * @author margaretha
 * @see UserGroupMember
 */
@Setter
@Getter
@Entity
@Table(name = "user_group")
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "name")
    private String name;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "deleted_by")
    private String deletedBy;
    
    @Enumerated(EnumType.STRING)
    private UserGroupStatus status;

    @OneToMany(mappedBy="group", fetch = FetchType.LAZY)
    List<UserGroupMember> members;

    @OneToMany(mappedBy = "userGroup", fetch = FetchType.LAZY)
    private List<VirtualCorpusAccess> virtualCorpusAccess;


    @Override
    public String toString () {
        return "id=" + id + ", name= " + name + ", createdBy= " + createdBy;
    }
}
