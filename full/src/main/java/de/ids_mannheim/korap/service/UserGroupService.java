package de.ids_mannheim.korap.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
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
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.ParameterChecker;
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
    @Autowired
    private AuthenticationManagerIface authManager;
    @Autowired
    private FullConfiguration config;

    private static List<Role> memberRoles;

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
        Collections.sort(userGroups);
        ArrayList<UserGroupDto> dtos = new ArrayList<>(userGroups.size());

        UserGroupMember userAsMember;
        List<UserGroupMember> members;
        for (UserGroup group : userGroups) {
            members = retrieveMembers(group.getId(), username);
            userAsMember =
                    groupMemberDao.retrieveMemberById(username, group.getId());
            dtos.add(converter.createUserGroupDto(group, members,
                    userAsMember.getStatus()));
        }

        return dtos;
    }

    private List<UserGroupMember> retrieveMembers (int groupId, String username)
            throws KustvaktException {
        List<UserGroupMember> groupAdmins = groupMemberDao.retrieveMemberByRole(
                groupId, PredefinedRole.USER_GROUP_ADMIN.getId());

        List<UserGroupMember> members = null;
        for (UserGroupMember admin : groupAdmins) {
            if (admin.getUserId().equals(username)) {
                members = groupMemberDao.retrieveMemberByGroupId(groupId);
                break;
            }
        }

        return members;
    }

    public UserGroup retrieveUserGroupById (int groupId)
            throws KustvaktException {
        return userGroupDao.retrieveGroupById(groupId);
    }

    public UserGroup retrieveHiddenGroup (int vcId) throws KustvaktException {
        return userGroupDao.retrieveHiddenGroupByVC(vcId);
    }

    public List<UserGroupMember> retrieveVCAccessAdmins (UserGroup userGroup)
            throws KustvaktException {
        List<UserGroupMember> groupAdmins = groupMemberDao.retrieveMemberByRole(
                userGroup.getId(), PredefinedRole.VC_ACCESS_ADMIN.getId());
        return groupAdmins;
    }

    public List<UserGroupMember> retrieveUserGroupAdmins (UserGroup userGroup)
            throws KustvaktException {
        List<UserGroupMember> groupAdmins = groupMemberDao.retrieveMemberByRole(
                userGroup.getId(), PredefinedRole.USER_GROUP_ADMIN.getId());
        return groupAdmins;
    }

    private void setMemberRoles () {
        if (memberRoles == null) {
            memberRoles = new ArrayList<Role>(2);
            memberRoles.add(roleDao.retrieveRoleById(
                    PredefinedRole.USER_GROUP_MEMBER.getId()));
            memberRoles.add(roleDao
                    .retrieveRoleById(PredefinedRole.VC_ACCESS_MEMBER.getId()));
        }
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
     * @param createdBy the user creating the group
     * @throws KustvaktException
     * 
     * 
     */
    public void createUserGroup (UserGroupJson groupJson, String createdBy)
            throws KustvaktException {

        int groupId = userGroupDao.createGroup(groupJson.getName(), createdBy,
                UserGroupStatus.ACTIVE);
        UserGroup userGroup = userGroupDao.retrieveGroupById(groupId);

        setMemberRoles();


        for (String memberId : groupJson.getMembers()) {
            if (memberId.equals(createdBy)) {
                // skip owner, already added while creating group.
                continue;
            }
            addGroupMember(memberId, userGroup, createdBy,
                    GroupMemberStatus.PENDING);
        }
    }

    public void deleteGroup (int groupId, String username)
            throws KustvaktException {
        User user = authManager.getUser(username);
        UserGroup userGroup = userGroupDao.retrieveGroupById(groupId);
        if (userGroup.getCreatedBy().equals(username) || user.isSystemAdmin()) {
            // soft delete
            userGroupDao.deleteGroup(groupId, username,
                    config.isSoftDeleteGroup());
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public int createAutoHiddenGroup (int vcId) throws KustvaktException {
        String groupName = "auto-hidden-group";
        int groupId = userGroupDao.createGroup(groupName, "system",
                UserGroupStatus.HIDDEN);

        return groupId;
    }

    public void deleteAutoHiddenGroup (int groupId, String deletedBy)
            throws KustvaktException {
        // default hard delete
        userGroupDao.deleteGroup(groupId, deletedBy, config.isSoftDeleteAutoGroup());
    }

    /** Adds a user to the specified usergroup. If the username with 
     *  {@link GroupMemberStatus} DELETED exists as a member of the group, 
     *  the entry will be deleted first, and a new entry will be added.
     *  
     *  If a username with other statuses exists, a KustvaktException will 
     *  be thrown.    
     * 
     * @see GroupMemberStatus
     * 
     * @param username a username
     * @param userGroup a user group
     * @param createdBy the user (VCA/system) adding the user the user-group 
     * @param status the status of the membership
     * @throws KustvaktException
     */
    public void addGroupMember (String username, UserGroup userGroup,
            String createdBy, GroupMemberStatus status)
            throws KustvaktException {

        int groupId = userGroup.getId();
        ParameterChecker.checkIntegerValue(groupId, "userGroupId");

        if (memberExists(username, groupId, status)) {
            throw new KustvaktException(StatusCodes.DB_ENTRY_EXISTS,
                    "Username " + username + " with status " + status
                            + " exists in user-group " + userGroup.getName(),
                    username, status.name(), userGroup.getName());
        }

        setMemberRoles();

        UserGroupMember member = new UserGroupMember();
        member.setCreatedBy(createdBy);
        member.setGroup(userGroup);
        member.setRoles(memberRoles);
        member.setStatus(status);
        member.setUserId(username);

        groupMemberDao.addMember(member);
    }

    private boolean memberExists (String username, int groupId,
            GroupMemberStatus status) throws KustvaktException {
        UserGroupMember existingMember;
        try {
            existingMember =
                    groupMemberDao.retrieveMemberById(username, groupId);
        }
        catch (KustvaktException e) {
            return false;
        }

        GroupMemberStatus existingStatus = existingMember.getStatus();
        if (existingStatus.equals(GroupMemberStatus.ACTIVE)
                || existingStatus.equals(status)) {
            return true;
        }
        else if (existingStatus.equals(GroupMemberStatus.DELETED)) {
            // hard delete, not customizable
            groupMemberDao.deleteMember(username, groupId, "system", false);
        }

        return false;
    }

    public void addUsersToGroup (UserGroupJson group, String username)
            throws KustvaktException {
        int groupId = group.getId();
        String[] members = group.getMembers();
        ParameterChecker.checkIntegerValue(groupId, "id");
        ParameterChecker.checkObjectValue(members, "members");

        UserGroup userGroup = retrieveUserGroupById(groupId);
        User user = authManager.getUser(username);
        if (isUserGroupAdmin(username, userGroup) || user.isSystemAdmin()) {
            for (String memberName : members) {
                addGroupMember(memberName, userGroup, username,
                        GroupMemberStatus.PENDING);
            }
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    private boolean isUserGroupAdmin (String username, UserGroup userGroup)
            throws KustvaktException {
        List<UserGroupMember> userGroupAdmins =
                retrieveUserGroupAdmins(userGroup);
        for (UserGroupMember admin : userGroupAdmins) {
            if (username.equals(admin.getUserId())) {
                return true;
            }
        }
        return false;
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
        groupMemberDao.deleteMember(username, groupId, username,
                config.isSoftDeleteGroupMember());
    }


    public boolean isMember (String username, UserGroup userGroup)
            throws KustvaktException {
        List<UserGroupMember> members =
                groupMemberDao.retrieveMemberByGroupId(userGroup.getId());
        for (UserGroupMember member : members) {
            if (member.getUserId().equals(username)
                    && member.getStatus().equals(GroupMemberStatus.ACTIVE)) {
                return true;
            }
        }
        return false;
    }

    public void deleteGroupMember (String memberId, int groupId,
            String deletedBy) throws KustvaktException {
        User user = authManager.getUser(deletedBy);
        UserGroup userGroup = userGroupDao.retrieveGroupById(groupId);
        if (memberId.equals(userGroup.getCreatedBy())) {
            throw new KustvaktException(StatusCodes.NOT_ALLOWED,
                    "Operation " + "'delete group owner'" + "is not allowed.",
                    "delete group owner");
        }
        else if (isUserGroupAdmin(deletedBy, userGroup)
                || user.isSystemAdmin()) {
            // soft delete
            groupMemberDao.deleteMember(memberId, groupId, deletedBy,
                    config.isSoftDeleteGroupMember());
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + deletedBy, deletedBy);
        }
    }

}
