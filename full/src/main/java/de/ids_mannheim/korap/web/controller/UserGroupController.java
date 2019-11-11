package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.UserGroupService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.input.UserGroupJson;

/**
 * UserGroupController defines web APIs related to user groups,
 * such as creating a user group, listing groups of a user,
 * adding members to a group and subscribing (confirming an
 * invitation) to a group.
 * 
 * These APIs are only available to logged-in users and not available
 * via third-party apps.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("{version}/group")
@ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
        BlockingFilter.class, PiwikFilter.class })
public class UserGroupController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;
    @Autowired
    private UserGroupService service;
    @Autowired
    private OAuth2ScopeService scopeService;

    /**
     * Returns all user-groups in which a user is an active or a
     * pending member.
     * 
     * Not suitable for system-admin, instead use {@link UserGroupController#
     * getUserGroupBySystemAdmin(SecurityContext, String, UserGroupStatus)}
     * 
     * @param securityContext
     * @return a list of user-groups
     * 
     */
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<UserGroupDto> getUserGroup (
            @Context SecurityContext securityContext) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.USER_GROUP_INFO);
            return service.retrieveUserGroupDto(context.getUsername());
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Lists user-groups for system-admin purposes. If username
     * parameter is not specified, list user-groups of all users. If
     * status is not specified, list user-groups of all statuses.
     * 
     * @param securityContext
     * @param username
     *            username
     * @param status
     *            {@link UserGroupStatus}
     * @return a list of user-groups
     */
    @GET
    @Path("list/system-admin")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<UserGroupDto> getUserGroupBySystemAdmin (
            @Context SecurityContext securityContext,
            @QueryParam("username") String username,
            @QueryParam("status") UserGroupStatus status) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.ADMIN);
            return service.retrieveUserGroupByStatus(username,
                    context.getUsername(), status);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Retrieves a specific user-group. Only system admins are
     * allowed.
     * 
     * @param securityContext
     * @param groupName
     *            group name
     * @return a user-group
     */
    @GET
    @Path("{groupName}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public UserGroupDto retrieveUserGroup (
            @Context SecurityContext securityContext,
            @PathParam("groupName") String groupName) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.ADMIN);
            return service.searchByName(context.getUsername(), groupName);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

    }

    /**
     * Creates a user group where the user in token context is the
     * group owner, and assigns the listed group members with status
     * GroupMemberStatus.PENDING.
     * 
     * Invitations must be sent to these proposed members. If a member
     * accepts the invitation, update his GroupMemberStatus to
     * GroupMemberStatus.ACTIVE by using
     * {@link UserGroupController#subscribeToGroup(SecurityContext, String)}.
     * 
     * If he rejects the invitation, update his GroupMemberStatus
     * to GroupMemberStatus.DELETED using
     * {@link UserGroupController#unsubscribeFromGroup(SecurityContext, String)}.
     * 
     * 
     * 
     * @param securityContext
     * @param group
     *            UserGroupJson
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUserGroup (@Context SecurityContext securityContext,
            UserGroupJson group) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.CREATE_USER_GROUP);
            service.createUserGroup(group, context.getUsername());
            return Response.ok("SUCCESS").build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Deletes a user-group specified by the group id. Only group
     * owner and system admins can delete groups.
     * 
     * @param securityContext
     * @param groupName the name of the group to delete
     * @return HTTP 200, if successful.
     */
    @DELETE
    @Path("{groupName}")
    public Response deleteUserGroup (@Context SecurityContext securityContext,
            @PathParam("groupName") String groupName) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.DELETE_USER_GROUP);
            service.deleteGroup(groupName, context.getUsername());
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }


    /**
     * Removes a user-group member. Group owner cannot be deleted.
     * Only group admins, system admins and the member himself can
     * remove a member. 
     * 
     * @param securityContext
     * @param memberUsername
     *            a username of a group member
     * @param groupName
     *            a group name
     * @return if successful, HTTP response status OK
     */
    @DELETE
    @Path("{groupName}/{memberUsername}")
    public Response removeUserFromGroup (
            @Context SecurityContext securityContext,
            @PathParam("memberUsername") String memberUsername,
            @PathParam("groupName") String groupName) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context,
                    OAuth2Scope.DELETE_USER_GROUP_MEMBER);
            service.deleteGroupMember(memberUsername, groupName,
                    context.getUsername());
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Invites group members to join a user-group specified in the
     * JSON object.
     * Only user-group admins and system admins are allowed.
     * 
     * @param securityContext
     * @param group
     *            UserGroupJson containing groupName and usernames to be
     *            invited as members
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("{groupName}/invite")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response inviteGroupMembers (
            @Context SecurityContext securityContext,
            @PathParam("groupName") String groupName,
            UserGroupJson group) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context,
                    OAuth2Scope.ADD_USER_GROUP_MEMBER);
            service.inviteGroupMembers(group, groupName, context.getUsername());
            return Response.ok("SUCCESS").build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Very similar to addMemberRoles web-service, but allows deletion
     * as well.
     * 
     * @param securityContext
     * @param groupName
     * @param memberUsername
     * @param roleIds
     * @return
     */
    @POST
    @Path("{groupName}/role/edit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response editMemberRoles (@Context SecurityContext securityContext,
            @PathParam("groupName") String groupName,
            @FormParam("memberUsername") String memberUsername,
            @FormParam("roleIds") List<Integer> roleIds) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context,
                    OAuth2Scope.EDIT_USER_GROUP_MEMBER_ROLE);
            service.editMemberRoles(context.getUsername(), groupName,
                    memberUsername, roleIds);
            return Response.ok("SUCCESS").build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Adds roles of an active member of a user-group. Only user-group
     * admins and system admins are allowed.
     * 
     * @param securityContext
     * @param groupName
     *            a group name
     * @param memberUsername
     *            a username of a group member
     * @param roleIds
     *            list of role ids
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("{groupName}/role/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addMemberRoles (@Context SecurityContext securityContext,
            @PathParam("groupName") String groupName,
            @FormParam("memberUsername") String memberUsername,
            @FormParam("roleIds") List<Integer> roleIds) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context,
                    OAuth2Scope.ADD_USER_GROUP_MEMBER_ROLE);
            service.addMemberRoles(context.getUsername(), groupName,
                    memberUsername, roleIds);
            return Response.ok("SUCCESS").build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Updates the roles of a member of a user-group by removing the
     * given roles. Only user-group admins and system admins are allowed.
     * 
     * @param securityContext
     * @param groupName
     *            a group name
     * @param memberUsername
     *            a username of a group member
     * @param roleIds
     *            list of role ids
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("{groupName}/role/delete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteMemberRoles (@Context SecurityContext securityContext,
            @PathParam("groupName") String groupName,
            @FormParam("memberUsername") String memberUsername,
            @FormParam("roleIds") List<Integer> roleIds) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context,
                    OAuth2Scope.DELETE_USER_GROUP_MEMBER_ROLE);
            service.deleteMemberRoles(context.getUsername(), groupName,
                    memberUsername, roleIds);
            return Response.ok("SUCCESS").build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Handles requests to accept membership invitation. Only invited
     * users can subscribe to the corresponding user-group.
     * 
     * @param securityContext
     * @param groupName
     *            a group name
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("{groupName}/subscribe")
    public Response subscribeToGroup (@Context SecurityContext securityContext,
            @PathParam("groupName") String groupName) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context,
                    OAuth2Scope.ADD_USER_GROUP_MEMBER);
            service.acceptInvitation(groupName, context.getUsername());
            return Response.ok("SUCCESS").build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Handles requests to reject membership invitation. A member can
     * only unsubscribe him/herself from a group.
     * 
     * Implemented identical to {@link #removeUserFromGroup(SecurityContext, String, String)}.
     * 
     * @param securityContext
     * @param groupName
     * @return if successful, HTTP response status OK
     */
    @DELETE
    @Path("{groupName}/unsubscribe")
    public Response unsubscribeFromGroup (
            @Context SecurityContext securityContext,
            @PathParam("groupName") String groupName) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context,
                    OAuth2Scope.DELETE_USER_GROUP_MEMBER);
            service.deleteGroupMember(context.getUsername(), groupName,
                    context.getUsername());
            return Response.ok("SUCCESS").build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
}
