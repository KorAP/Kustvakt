package de.ids_mannheim.korap.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.PredefinedUserGroup;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.dao.RoleDao;
import de.ids_mannheim.korap.dao.UserGroupDao;
import de.ids_mannheim.korap.dao.UserGroupMemberDao;
import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.dto.converter.UserGroupConverter;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.controller.UserGroupController;
import de.ids_mannheim.korap.web.input.UserGroupJson;

/** UserGroupService defines the logic behind user group web controller.
 * 
 * @see UserGroupController
 * 
 * @author margaretha
 *
 */
@Service
public class UserGroupService {

    @Autowired
    private UserGroupDao userGroupDao;
    @Autowired
    private UserGroupMemberDao groupMemberDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private UserGroupConverter converter;


    /** Only users with {@link PredefinedRole#USER_GROUP_ADMIN} 
     * are allowed to see the members of the group.
     * 
     * @param username username
     * @return a list of usergroups
     * @throws KustvaktException
     * 
     * @see {@link PredefinedRole}
     */
    public List<UserGroupDto> retrieveUserGroup (String username)
            throws KustvaktException {

        List<UserGroup> userGroups =
                userGroupDao.retrieveGroupByUserId(username);

        ArrayList<UserGroupDto> dtos = new ArrayList<>(userGroups.size());

        List<UserGroupMember> groupAdmins;
        for (UserGroup group : userGroups) {
            groupAdmins = groupMemberDao.retrieveMemberByRole(group.getId(),
                    PredefinedRole.USER_GROUP_ADMIN.getId());

            List<UserGroupMember> members = null;
            for (UserGroupMember admin : groupAdmins) {
                if (admin.getUserId().equals(username)) {
                    members = groupMemberDao
                            .retrieveMemberByGroupId(group.getId());
                    break;
                }
            }
            dtos.add(converter.createUserGroupDto(group, members));
        }

        return dtos;
    }
    
    public UserGroup retrieveUserGroupById (int groupId)
            throws KustvaktException {
        return userGroupDao.retrieveGroupById(groupId);
    }
    
    public UserGroup retrieveAllUserGroup () {
        return userGroupDao.retrieveAllUserGroup();
    }

    /** Group owner is automatically added when creating a group. 
     *  Do not include owners in group members. 
     *  
     *  {@link PredefinedRole#USER_GROUP_MEMBER} and 
     *  {@link PredefinedRole#VC_ACCESS_MEMBER} roles are 
     *  automatically assigned to each group member. 
     *  
     *  {@link PredefinedRole#USER_GROUP_MEMBER} restrict users 
     *  to see other group members and allow users to remove 
     *  themselves from the groups.
     *   
     *  {@link PredefinedRole#VC_ACCESS_MEMBER} allow user to 
     *  read group VC.
     * 
     * @see /full/src/main/resources/db/predefined/V3.2__insert_predefined_roles.sql
     * 
     * @param groupJson UserGroupJson object from json
     * @param username the user creating the group
     * @throws KustvaktException
     * 
     * 
     */
    public void createUserGroup (UserGroupJson groupJson, String username)
            throws KustvaktException {

        int groupId = userGroupDao.createGroup(groupJson.getName(), username,
                UserGroupStatus.ACTIVE);
        UserGroup group = userGroupDao.retrieveGroupById(groupId);

        List<Role> roles = new ArrayList<Role>(2);
        roles.add(roleDao
                .retrieveRoleById(PredefinedRole.USER_GROUP_MEMBER.getId()));
        roles.add(roleDao
                .retrieveRoleById(PredefinedRole.VC_ACCESS_MEMBER.getId()));

        UserGroupMember m;
        for (String memberId : groupJson.getMembers()) {
            if (memberId.equals(username)) {
                // skip owner, already added while creating group.
                continue;
            }

            m = new UserGroupMember();
            m.setUserId(memberId);
            m.setCreatedBy(username);
            m.setGroup(group);
            m.setStatus(GroupMemberStatus.PENDING);
            m.setRoles(roles);
        }
    }

    public int createAutoHiddenGroup (int vcId) throws KustvaktException {
        String groupName = "auto-published-group";
        int groupId = userGroupDao.createGroup(groupName, "system",
                UserGroupStatus.HIDDEN);

        return groupId;
    }

    /** Updates the {@link GroupMemberStatus} of a pending member 
     * to {@link GroupMemberStatus#ACTIVE}.
     * 
     * @param groupId groupId
     * @param username the username of the group member
     * @throws KustvaktException
     */
    public void subscribe (int groupId, String username)
            throws KustvaktException {
        groupMemberDao.approveMember(username, groupId);
    }


    /** Updates the {@link GroupMemberStatus} of a member to 
     * {@link GroupMemberStatus#DELETED}
     * 
     * @param groupId groupId
     * @param username member's username
     * @throws KustvaktException
     */
    public void unsubscribe (int groupId, String username)
            throws KustvaktException {
        groupMemberDao.deleteMember(username, groupId, true);
    }

}
