package de.ids_mannheim.korap.service;

import java.sql.SQLException;
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

import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.PrivilegeType;
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
    private RandomCodeGenerator random;

    /**
     * Only users with {@link PredefinedRole#GROUP_ADMIN}
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

        List<UserGroup> userGroups = userGroupDao
                .retrieveGroupByUserId(username);
        Collections.sort(userGroups);
        return userGroups;
    }

    public List<UserGroupDto> retrieveUserGroupDto (String username)
            throws KustvaktException {
        List<UserGroup> userGroups = retrieveUserGroup(username);

        ArrayList<UserGroupDto> dtos = new ArrayList<>(userGroups.size());
        List<UserGroupMember> members;
        UserGroupDto groupDto;
        for (UserGroup group : userGroups) {
            members = retrieveMembers(group.getId(), username);
            groupDto = converter.createUserGroupDto(group, members);
            dtos.add(groupDto);
        }

        return dtos;

    }

    private List<UserGroupMember> retrieveMembers (int groupId, String username)
            throws KustvaktException {
        List<UserGroupMember> groupAdmins = groupMemberDao.retrieveMemberByRole(
                groupId, PredefinedRole.GROUP_ADMIN);

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

    public UserGroup retrieveHiddenUserGroupByQueryId (int queryId)
            throws KustvaktException {
        return userGroupDao.retrieveHiddenGroupByQueryId(queryId);
    }
    
    public UserGroupDto retrieveHiddenUserGroupByQueryName (String queryName)
            throws KustvaktException {
        UserGroup group = userGroupDao
                .retrieveHiddenGroupByQueryName(queryName);
        List<UserGroupMember> members = groupMemberDao
                .retrieveMemberByGroupId(group.getId());
        return converter.createUserGroupDto(group, members);
    }

    public List<UserGroupDto> retrieveUserGroupByStatus (String username,
            UserGroupStatus status) throws KustvaktException {

        List<UserGroup> userGroups = userGroupDao
                .retrieveGroupByStatus(username, status);
        Collections.sort(userGroups);
        ArrayList<UserGroupDto> dtos = new ArrayList<>(userGroups.size());

        List<UserGroupMember> members;
        UserGroupDto groupDto;
        for (UserGroup group : userGroups) {
            members = groupMemberDao.retrieveMemberByGroupId(group.getId());
            groupDto = converter.createUserGroupDto(group, members);
            dtos.add(groupDto);
        }
        return dtos;
    }
    
    private Set<Role> prepareMemberRoles (UserGroup userGroup) {
            Role r1 = new Role(PredefinedRole.GROUP_MEMBER,
                    PrivilegeType.DELETE_SELF, userGroup);
            roleDao.addRole(r1);
            Set<Role>memberRoles = new HashSet<Role>();
            memberRoles.add(r1);
            
            Set<Role> roles = 
                    roleDao.retrieveRolesByGroupIdWithUniqueQuery(userGroup.getId());
            for(Role r :roles) {
                memberRoles.add(r);
            }
            return memberRoles;
    }

    /**
     * Group owner is automatically added when creating a group.
     * Do not include owners in group members.
     * 
     * {@link PredefinedRole#GROUP_MEMBER} and
     * {@link PredefinedRole#VC_ACCESS_MEMBER} roles are
     * automatically assigned to each group member.
     * 
     * {@link PredefinedRole#GROUP_MEMBER} restrict users
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
    public boolean createUpdateUserGroup (String groupName, String description,
            String createdBy) throws KustvaktException {
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
        try {
            userGroup = retrieveUserGroupByName(groupName);
            groupExists = true;
        }
        catch (KustvaktException e) {
            if (e.getStatusCode() != StatusCodes.NO_RESOURCE_FOUND) {
                throw e;
            }
        }

        if (!groupExists) {
            try {
                userGroupDao.createGroup(groupName, description, createdBy,
                        UserGroupStatus.ACTIVE);
                userGroup = retrieveUserGroupByName(groupName);
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
        UserGroup userGroup = retrieveUserGroupByName(groupName);
        if (userGroup.getCreatedBy().equals(username)
                || adminDao.isAdmin(username)) {
            userGroupDao.deleteGroup(userGroup.getId(), username);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public int createAutoHiddenGroup (String queryCreator, String queryName) 
            throws KustvaktException {
        String code = random.createRandomCode();
        String groupName = "auto-" + code;
        int groupId = userGroupDao.createGroup(groupName, "auto-hidden-group for "
                + "~"+queryCreator+"/"+queryName,
                "system", UserGroupStatus.HIDDEN);

        return groupId;
    }

    public void addGroupMember (String username, UserGroup userGroup,
            String createdBy, Set<Role> roles)
            throws KustvaktException {
        
        if (!isMember(username, userGroup)) {
            int groupId = userGroup.getId();
            ParameterChecker.checkIntegerValue(groupId, "userGroupId");
    
            UserGroupMember member = new UserGroupMember();
            member.setGroup(userGroup);
            member.setUserId(username);
            if (roles !=null) {
                member.setRoles(roles);
            }
            groupMemberDao.addMember(member);
        }
        else {
            throw new KustvaktException(StatusCodes.GROUP_MEMBER_EXISTS,
                    "Username: "+username+" exists in the user-group: "+
                    userGroup.getName(), username, userGroup.getName());
        }
    }

    public void addGroupMembers (String groupName, String groupMembers,
            String username) throws KustvaktException {
        String[] members = groupMembers.split(",");
        ParameterChecker.checkStringValue(groupName, "group name");
        ParameterChecker.checkStringValue(groupMembers, "members");

        UserGroup userGroup = retrieveUserGroupByName(groupName);
        if (isUserGroupAdmin(username, userGroup)
                || adminDao.isAdmin(username)) {
            Set<Role> memberRoles = prepareMemberRoles(userGroup);
            for (String memberName : members) {
                addGroupMember(memberName, userGroup, username,memberRoles);
            }
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }

    public boolean isUserGroupAdmin (String username, UserGroup userGroup)
            throws KustvaktException {

        List<UserGroupMember> userGroupAdmins = groupMemberDao
                .retrieveMemberByRole(userGroup.getId(),
                        PredefinedRole.GROUP_ADMIN);

        for (UserGroupMember admin : userGroupAdmins) {
            if (username.equals(admin.getUserId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isMember (String username, UserGroup userGroup)
            throws KustvaktException {
        List<UserGroupMember> members = groupMemberDao
                .retrieveMemberByGroupId(userGroup.getId());
        for (UserGroupMember member : members) {
            if (member.getUserId().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public void deleteGroupMember (String memberId, String groupName,
            String deletedBy) throws KustvaktException {

        UserGroup userGroup = retrieveUserGroupByName(groupName);
        if (memberId.equals(userGroup.getCreatedBy())) {
            throw new KustvaktException(StatusCodes.NOT_ALLOWED,
                    "Operation " + "'delete group owner'" + "is not allowed.",
                    "delete group owner");
        }
        else if (memberId.equals(deletedBy)
                || isUserGroupAdmin(deletedBy, userGroup)
                || adminDao.isAdmin(deletedBy)) {
            UserGroupMember member = groupMemberDao.retrieveMemberById(memberId,
                    userGroup.getId());
            groupMemberDao.deleteMember(member, deletedBy);
        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + deletedBy, deletedBy);
        }
    }

    public UserGroupDto searchByName (String groupName)
            throws KustvaktException {
        UserGroup userGroup = userGroupDao.retrieveGroupByName(groupName, true);
        UserGroupDto groupDto = converter.createUserGroupDto(userGroup,
                userGroup.getMembers());
        return groupDto;
    }

    public void addAdminRole (String username, String groupName,
            String memberUsername) throws KustvaktException {
        ParameterChecker.checkStringValue(username, "username");
        ParameterChecker.checkStringValue(groupName, "groupName");
        ParameterChecker.checkStringValue(memberUsername, "memberUsername");

        UserGroup userGroup = userGroupDao.retrieveGroupByName(groupName, true);

        if (isUserGroupAdmin(username, userGroup)
                || adminDao.isAdmin(username)) {

            UserGroupMember member = groupMemberDao
                    .retrieveMemberById(memberUsername, userGroup.getId());

            if (!isUserGroupAdmin(memberUsername, userGroup)) {
                Set<Role> existingRoles = member.getRoles();
                PredefinedRole role = PredefinedRole.GROUP_ADMIN;

                Role r1 = new Role(role, PrivilegeType.READ_MEMBER, userGroup);
                roleDao.addRole(r1);
                existingRoles.add(r1);

                Role r2 = new Role(role, PrivilegeType.DELETE_MEMBER,
                        userGroup);
                roleDao.addRole(r2);
                existingRoles.add(r2);

                Role r3 = new Role(role, PrivilegeType.WRITE_MEMBER, userGroup);
                roleDao.addRole(r3);
                existingRoles.add(r3);

                Role r4 = new Role(role, PrivilegeType.SHARE_QUERY, userGroup);
                roleDao.addRole(r4);
                existingRoles.add(r4);

                Role r5 = new Role(role, PrivilegeType.DELETE_QUERY, userGroup);
                roleDao.addRole(r5);
                existingRoles.add(r5);

                member.setRoles(existingRoles);
                groupMemberDao.updateMember(member);
            }
            else {
                throw new KustvaktException(StatusCodes.GROUP_ADMIN_EXISTS,
                        "Username " + memberUsername
                         + " is already a group admin.");
            }

        }
        else {
            throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: " + username, username);
        }
    }
    
    public void deleteMemberRoles (String username, String groupName,
            String memberUsername, List<PredefinedRole> rolesToBeDeleted)
            throws KustvaktException {

        ParameterChecker.checkStringValue(username, "username");
        ParameterChecker.checkStringValue(groupName, "groupName");
        ParameterChecker.checkStringValue(memberUsername, "memberUsername");

        UserGroup userGroup = userGroupDao.retrieveGroupByName(groupName, true);

        if (isUserGroupAdmin(username, userGroup)
                || adminDao.isAdmin(username)) {

            UserGroupMember member = groupMemberDao
                    .retrieveMemberById(memberUsername, userGroup.getId());

            Set<Role> roles = member.getRoles();
            Iterator<Role> i = roles.iterator();
            while (i.hasNext()) {
                if (rolesToBeDeleted.contains(i.next().getName())) {
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
