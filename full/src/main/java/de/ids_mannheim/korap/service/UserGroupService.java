package de.ids_mannheim.korap.service;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.dao.RoleDao;
import de.ids_mannheim.korap.dao.UserGroupDao;
import de.ids_mannheim.korap.dao.UserGroupMemberDao;
import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.dto.converter.UserGroupConverter;
import de.ids_mannheim.korap.encryption.RandomCodeGenerator;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.ParameterChecker;
import de.ids_mannheim.korap.web.controller.UserGroupController;

/**
 * UserGroupService defines the logic behind user group web
 * controller.
 * 
 * @see UserGroupController
 * 
 * @author margaretha
 *
 */
@Service
public class UserGroupService {

    public static Logger jlog = LogManager.getLogger(UserGroupService.class);
    public static boolean DEBUG = false;
    
    public static Pattern groupNamePattern = Pattern
            .compile("[a-zA-Z0-9]+[a-zA-Z_0-9-.]+");
    
    @Autowired
    private UserGroupDao userGroupDao;
    @Autowired
    private UserGroupMemberDao groupMemberDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private AdminDao adminDao;
    @Autowired
    private UserGroupConverter converter;
    @Autowired
    private FullConfiguration config;
    @Autowired
    private MailService mailService;
    @Autowired
    private RandomCodeGenerator random;
    
    private static Set<Role> memberRoles;

    /**
     * Only users with {@link PredefinedRole#USER_GROUP_ADMIN}
     * are allowed to see the members of the group.
     * 
     * @param username
     *            username
     * @return a list of usergroups
     * @throws KustvaktException
     * 
     * @see {@link PredefinedRole}
     */
    public List<UserGroup> retrieveUserGroup (String username)
            throws KustvaktException {

        List<UserGroup> userGroups =
                userGroupDao.retrieveGroupByUserId(username);
        Collections.sort(userGroups);
        return userGroups;
    }
    
    public List<UserGroupDto> retrieveUserGroupDto (String username)
            throws KustvaktException {
        List<UserGroup> userGroups = retrieveUserGroup(username);
        
        ArrayList<UserGroupDto> dtos = new ArrayList<>(userGroups.size());
        UserGroupMember userAsMember;
        List<UserGroupMember> members;
        UserGroupDto groupDto;
        for (UserGroup group : userGroups) {
            members = retrieveMembers(group.getId(), username);
            userAsMember =
                    groupMemberDao.retrieveMemberById(username, group.getId());
            groupDto = converter.createUserGroupDto(group, members,
                    userAsMember.getStatus(), userAsMember.getRoles());
            dtos.add(groupDto);
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
    
    public UserGroup retrieveUserGroupByName (String groupName)
            throws KustvaktException {
        return userGroupDao.retrieveGroupByName(groupName, false);
    }

    public UserGroup retrieveHiddenUserGroupByQuery (int queryId)
            throws KustvaktException {
        return userGroupDao.retrieveHiddenGroupByQuery(queryId);
    }

    public List<UserGroupDto> retrieveUserGroupByStatus (String username,
            String contextUsername, UserGroupStatus status)
            throws KustvaktException {

        boolean isAdmin = adminDao.isAdmin(contextUsername);

        if (isAdmin) {
            List<UserGroup> userGroups =
                    userGroupDao.retrieveGroupByStatus(username, status);
            Collections.sort(userGroups);
            ArrayList<UserGroupDto> dtos = new ArrayList<>(userGroups.size());

            List<UserGroupMember> members;
            UserGroupDto groupDto;
            for (UserGroup group : userGroups) {
                members = groupMemberDao.retrieveMemberByGroupId(group.getId(),
                        true);
                groupDto = converter.createUserGroupDto(group, members, null,
                        null);
                dtos.add(groupDto);
            }
            return dtos;
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + contextUsername,
                    contextUsername);
        }
    }

    public List<UserGroupMember> retrieveQueryAccessAdmins (UserGroup userGroup)
            throws KustvaktException {
        List<UserGroupMember> groupAdmins = groupMemberDao.retrieveMemberByRole(
                userGroup.getId(), PredefinedRole.VC_ACCESS_ADMIN.getId());
        return groupAdmins;
    }

    private void setMemberRoles () {
        if (memberRoles == null) {
            memberRoles = new HashSet<Role>(2);
            memberRoles.add(roleDao.retrieveRoleById(
                    PredefinedRole.USER_GROUP_MEMBER.getId()));
            memberRoles.add(roleDao
                    .retrieveRoleById(PredefinedRole.VC_ACCESS_MEMBER.getId()));
        }
    }

    /**
     * Group owner is automatically added when creating a group.
     * Do not include owners in group members.
     * 
     * {@link PredefinedRole#USER_GROUP_MEMBER} and
     * {@link PredefinedRole#VC_ACCESS_MEMBER} roles are
     * automatically assigned to each group member.
     * 
     * {@link PredefinedRole#USER_GROUP_MEMBER} restrict users
     * to see other group members and allow users to remove
     * themselves from the groups.
     * 
     * {@link PredefinedRole#VC_ACCESS_MEMBER} allow user to
     * read group query.
     * 
     * @see /full/src/main/resources/db/predefined/V3.2__insert_predefined_roles.sql
     * 
     * @param createdBy
     *            the user creating the group
     * @throws KustvaktException
     * 
     * 
     */
    public boolean createUpdateUserGroup (String groupName, String description, String createdBy)
            throws KustvaktException {
        ParameterChecker.checkNameValue(groupName, "groupName");
        ParameterChecker.checkStringValue(createdBy, "createdBy");

        if (!groupNamePattern.matcher(groupName).matches()) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "User-group name must consists of alphanumerical characters "
                            + "(limited to ASCII), underscores, dashes and periods. "
                            + "The name has to start with an alphanumerical character.",
                    groupName);
        }
        
        UserGroup userGroup = null;
        boolean groupExists = false;
        try{
            userGroup = userGroupDao.retrieveGroupByName(groupName,false);
            groupExists = true;
        }
        catch (KustvaktException e) {
            if (e.getStatusCode() != StatusCodes.NO_RESOURCE_FOUND){
                throw e;
            }
        }
        
        if (!groupExists){
            try {
                userGroupDao.createGroup(groupName, description, createdBy,
                        UserGroupStatus.ACTIVE);
                userGroup = userGroupDao.retrieveGroupByName(groupName,false);
            }
            // handle DB exceptions, e.g. unique constraint
            catch (Exception e) {
                Throwable cause = e;
                Throwable lastCause = null;
                while ((cause = cause.getCause()) != null
                        && !cause.equals(lastCause)) {
                    if (cause instanceof SQLException) {
                        break;
                    }
                    lastCause = cause;
                }
                throw new KustvaktException(StatusCodes.DB_INSERT_FAILED,
                        cause.getMessage());
            }
        }
        else if (description != null) {
            userGroup.setDescription(description);
            userGroupDao.updateGroup(userGroup);
        }
        return groupExists;
    }

    public void deleteGroup (String groupName, String username)
            throws KustvaktException {
        UserGroup userGroup = userGroupDao.retrieveGroupByName(groupName,false);
        if (userGroup.getStatus() == UserGroupStatus.DELETED) {
            // EM: should this be "not found" instead?
            throw new KustvaktException(StatusCodes.GROUP_DELETED,
                    "Group " + userGroup.getName() + " has been deleted.",
                    userGroup.getName());
        }
        else if (userGroup.getCreatedBy().equals(username)
                || adminDao.isAdmin(username)) {
            // soft delete
            userGroupDao.deleteGroup(userGroup.getId(), username,
                    config.isSoftDeleteGroup());
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public int createAutoHiddenGroup () throws KustvaktException {
        String code = random.createRandomCode();
        String groupName = "auto-"+code;
        int groupId = userGroupDao.createGroup(groupName, "auto-hidden-group",
                "system", UserGroupStatus.HIDDEN);

        return groupId;
    }

    public void deleteAutoHiddenGroup (int groupId, String deletedBy)
            throws KustvaktException {
        // default hard delete
        userGroupDao.deleteGroup(groupId, deletedBy,
                config.isSoftDeleteAutoGroup());
    }

    /**
     * Adds a user to the specified usergroup. If the username with
     * {@link GroupMemberStatus} DELETED exists as a member of the
     * group,
     * the entry will be deleted first, and a new entry will be added.
     * 
     * If a username with other statuses exists, a KustvaktException
     * will
     * be thrown.
     * 
     * @see GroupMemberStatus
     * 
     * @param username
     *            a username
     * @param userGroup
     *            a user group
     * @param createdBy
     *            the user (query-access admin/system) adding the user the user-group
     * @param status
     *            the status of the membership
     * @throws KustvaktException
     */
    public void inviteGroupMember (String username, UserGroup userGroup,
            String createdBy, GroupMemberStatus status)
            throws KustvaktException {

        addGroupMember(username, userGroup, createdBy, status);

        if (config.isMailEnabled()
                && userGroup.getStatus() != UserGroupStatus.HIDDEN) {
            mailService.sendMemberInvitationNotification(username,
                    userGroup.getName(), createdBy);
        }
    }

    public void addGroupMember (String username, UserGroup userGroup,
            String createdBy, GroupMemberStatus status)
            throws KustvaktException {
        int groupId = userGroup.getId();
        ParameterChecker.checkIntegerValue(groupId, "userGroupId");

        GroupMemberStatus existingStatus =
                memberExists(username, groupId, status);
        if (existingStatus != null) {
            throw new KustvaktException(StatusCodes.GROUP_MEMBER_EXISTS,
                    "Username " + username + " with status " + existingStatus
                            + " exists in the user-group "
                            + userGroup.getName(),
                    username, existingStatus.name(), userGroup.getName());
        }

        UserGroupMember member = new UserGroupMember();
        member.setCreatedBy(createdBy);
        member.setGroup(userGroup);
        member.setStatus(status);
        member.setUserId(username);
        groupMemberDao.addMember(member);
    }

    private GroupMemberStatus memberExists (String username, int groupId,
            GroupMemberStatus status) throws KustvaktException {
        UserGroupMember existingMember;
        try {
            existingMember =
                    groupMemberDao.retrieveMemberById(username, groupId);
        }
        catch (KustvaktException e) {
            return null;
        }

        GroupMemberStatus existingStatus = existingMember.getStatus();
        if (existingStatus.equals(GroupMemberStatus.ACTIVE)
                || existingStatus.equals(status)) {
            return existingStatus;
        }
        else if (existingStatus.equals(GroupMemberStatus.DELETED)) {
            // hard delete, not customizable
            doDeleteMember(username, groupId, "system", false);
        }

        return null;
    }

    public void inviteGroupMembers (String groupName, String groupMembers,
            String inviter) throws KustvaktException {
        String[] members = groupMembers.split(",");
        ParameterChecker.checkStringValue(groupName, "group name");
        ParameterChecker.checkStringValue(groupMembers, "members");

        UserGroup userGroup = retrieveUserGroupByName(groupName);
        if (userGroup.getStatus() == UserGroupStatus.DELETED) {
            throw new KustvaktException(StatusCodes.GROUP_DELETED,
                    "Group " + userGroup.getName() + " has been deleted.",
                    userGroup.getName());
        }

        if (isUserGroupAdmin(inviter, userGroup) || adminDao.isAdmin(inviter)) {
            for (String memberName : members) {
                inviteGroupMember(memberName, userGroup, inviter,
                        GroupMemberStatus.PENDING);
            }
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + inviter, inviter);
        }
    }

    private boolean isUserGroupAdmin (String username, UserGroup userGroup)
            throws KustvaktException {

        List<UserGroupMember> userGroupAdmins =
                groupMemberDao.retrieveMemberByRole(userGroup.getId(),
                        PredefinedRole.USER_GROUP_ADMIN.getId());

        for (UserGroupMember admin : userGroupAdmins) {
            if (username.equals(admin.getUserId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the {@link GroupMemberStatus} of a pending member
     * to {@link GroupMemberStatus#ACTIVE} and add default member
     * roles.
     * 
     * @param groupId
     *            groupId
     * @param username
     *            the username of the group member
     * @throws KustvaktException
     */
    public void acceptInvitation (String groupName, String username)
            throws KustvaktException {

        ParameterChecker.checkStringValue(username, "userId");
        ParameterChecker.checkStringValue(groupName, "groupId");

        UserGroup userGroup = userGroupDao.retrieveGroupByName(groupName, false);
        if (userGroup.getStatus() == UserGroupStatus.DELETED) {
            throw new KustvaktException(StatusCodes.GROUP_DELETED,
                    "Group " + userGroup.getName() + " has been deleted.",
                    userGroup.getName());
        }

        UserGroupMember member =
                groupMemberDao.retrieveMemberById(username, userGroup.getId());
        GroupMemberStatus status = member.getStatus();
        if (status.equals(GroupMemberStatus.DELETED)) {
            throw new KustvaktException(StatusCodes.GROUP_MEMBER_DELETED,
                    username + " has already been deleted from the group "
                            + userGroup.getName(),
                    username, userGroup.getName());
        }
        else if (member.getStatus().equals(GroupMemberStatus.ACTIVE)) {
            throw new KustvaktException(StatusCodes.GROUP_MEMBER_EXISTS,
                    "Username " + username + " with status " + status
                            + " exists in the user-group "
                            + userGroup.getName(),
                    username, status.name(), userGroup.getName());
        }
        // status pending
        else {
            if (DEBUG) {
                jlog.debug("status: " + member.getStatusDate());
            }
            ZonedDateTime expiration = member.getStatusDate().plusMinutes(30);
            ZonedDateTime now = ZonedDateTime.now();
            if (DEBUG) {
                jlog.debug("expiration: " + expiration + ", now: " + now);
            }

            if (expiration.isAfter(now)) {
                member.setStatus(GroupMemberStatus.ACTIVE);
                setMemberRoles();
                member.setRoles(memberRoles);
                groupMemberDao.updateMember(member);
            }
            else {
                throw new KustvaktException(StatusCodes.INVITATION_EXPIRED);
            }
        }
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

    public void deleteGroupMember (String memberId, String groupName,
            String deletedBy) throws KustvaktException {

        UserGroup userGroup = userGroupDao.retrieveGroupByName(groupName, false);
        if (userGroup.getStatus() == UserGroupStatus.DELETED) {
            throw new KustvaktException(StatusCodes.GROUP_DELETED,
                    "Group " + userGroup.getName() + " has been deleted.",
                    userGroup.getName());
        }
        else if (memberId.equals(userGroup.getCreatedBy())) {
            throw new KustvaktException(StatusCodes.NOT_ALLOWED,
                    "Operation " + "'delete group owner'" + "is not allowed.",
                    "delete group owner");
        }
        else if (memberId.equals(deletedBy)
                || isUserGroupAdmin(deletedBy, userGroup)
                || adminDao.isAdmin(deletedBy)) {
            // soft delete
            doDeleteMember(memberId, userGroup.getId(), deletedBy,
                    config.isSoftDeleteGroupMember());
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + deletedBy, deletedBy);
        }
    }

    /**
     * Updates the {@link GroupMemberStatus} of a member to
     * {@link GroupMemberStatus#DELETED}
     * 
     * @param userId
     *            user to be deleted
     * @param groupId
     *            user-group id
     * @param deletedBy
     *            user that issue the delete
     * @param isSoftDelete
     *            true if database entry is to be deleted
     *            permanently, false otherwise
     * @throws KustvaktException
     */
    private void doDeleteMember (String username, int groupId, String deletedBy,
            boolean isSoftDelete) throws KustvaktException {

        UserGroup group = userGroupDao.retrieveGroupById(groupId);

        UserGroupMember member =
                groupMemberDao.retrieveMemberById(username, groupId);
        GroupMemberStatus status = member.getStatus();
        if (isSoftDelete && status.equals(GroupMemberStatus.DELETED)) {
            throw new KustvaktException(StatusCodes.GROUP_MEMBER_DELETED,
                    username + " has already been deleted from the group "
                            + group.getName(),
                    username, group.getName());
        }

        groupMemberDao.deleteMember(member, deletedBy, isSoftDelete);
    }

    public UserGroupDto searchByName (String username, String groupName)
            throws KustvaktException {
        if (adminDao.isAdmin(username)) {
            UserGroup userGroup =
                    userGroupDao.retrieveGroupByName(groupName, true);
            UserGroupDto groupDto = converter.createUserGroupDto(userGroup,
                    userGroup.getMembers(), null, null);
            return groupDto;
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }

    }

    public void editMemberRoles (String username, String groupName,
            String memberUsername, List<Integer> roleIds)
            throws KustvaktException {

        ParameterChecker.checkStringValue(username, "username");
        ParameterChecker.checkStringValue(groupName, "groupName");
        ParameterChecker.checkStringValue(memberUsername, "memberUsername");

        UserGroup userGroup = userGroupDao.retrieveGroupByName(groupName, true);
        UserGroupStatus groupStatus = userGroup.getStatus();
        if (groupStatus == UserGroupStatus.DELETED) {
            throw new KustvaktException(StatusCodes.GROUP_DELETED,
                    "Usergroup has been deleted.");
        }
        else if (isUserGroupAdmin(username, userGroup)
                || adminDao.isAdmin(username)) {

            UserGroupMember member =
                    groupMemberDao.retrieveMemberById(memberUsername,
                            userGroup.getId());

            if (!member.getStatus().equals(GroupMemberStatus.ACTIVE)) {
                throw new KustvaktException(StatusCodes.GROUP_MEMBER_INACTIVE,
                        memberUsername + " has status " + member.getStatus(),
                        memberUsername, member.getStatus().name());
            }

            Set<Role> roles = new HashSet<>();
            for (int i = 0; i < roleIds.size(); i++) {
                roles.add(roleDao.retrieveRoleById(roleIds.get(i)));
            }
            member.setRoles(roles);
            groupMemberDao.updateMember(member);

        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public void addMemberRoles (String username, String groupName,
            String memberUsername, List<Integer> roleIds)
            throws KustvaktException {

        ParameterChecker.checkStringValue(username, "username");
        ParameterChecker.checkStringValue(groupName, "groupName");
        ParameterChecker.checkStringValue(memberUsername, "memberUsername");

        UserGroup userGroup = userGroupDao.retrieveGroupByName(groupName, true);
        UserGroupStatus groupStatus = userGroup.getStatus();
        if (groupStatus == UserGroupStatus.DELETED) {
            throw new KustvaktException(StatusCodes.GROUP_DELETED,
                    "Usergroup has been deleted.");
        }
        else if (isUserGroupAdmin(username, userGroup)
                || adminDao.isAdmin(username)) {

            UserGroupMember member =
                    groupMemberDao.retrieveMemberById(memberUsername,
                            userGroup.getId());

            if (!member.getStatus().equals(GroupMemberStatus.ACTIVE)) {
                throw new KustvaktException(StatusCodes.GROUP_MEMBER_INACTIVE,
                        memberUsername + " has status " + member.getStatus(),
                        memberUsername, member.getStatus().name());
            }

            Set<Role> roles = member.getRoles();
            for (int i = 0; i < roleIds.size(); i++) {
                roles.add(roleDao.retrieveRoleById(roleIds.get(i)));
            }
            member.setRoles(roles);
            groupMemberDao.updateMember(member);

        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public void deleteMemberRoles (String username, String groupName,
            String memberUsername, List<Integer> roleIds)
            throws KustvaktException {

        ParameterChecker.checkStringValue(username, "username");
        ParameterChecker.checkStringValue(groupName, "groupName");
        ParameterChecker.checkStringValue(memberUsername, "memberUsername");

        UserGroup userGroup = userGroupDao.retrieveGroupByName(groupName, true);

        if (isUserGroupAdmin(username, userGroup)
                || adminDao.isAdmin(username)) {

            UserGroupMember member =
                    groupMemberDao.retrieveMemberById(memberUsername,
                            userGroup.getId());

            Set<Role> roles = member.getRoles();
            Iterator<Role> i = roles.iterator();
            while (i.hasNext()) {
                if (roleIds.contains(i.next().getId())) {
                    i.remove();
                }
            }

            member.setRoles(roles);
            groupMemberDao.updateMember(member);

        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }
}
