package de.ids_mannheim.korap.web.controller;

import java.util.List;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.UserGroupService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.utils.ResourceFilters;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

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
        BlockingFilter.class})
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
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
     * Not suitable for system-admin, instead use
     * {@link UserGroupController#
     * getUserGroupBySystemAdmin(SecurityContext, String,
     * UserGroupStatus)}
     * 
     * @param securityContext
     * @return a list of user-groups
     * 
     */
    @GET
    public List<UserGroupDto> listUserGroups (
            @Context SecurityContext securityContext) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.USER_GROUP_INFO);
            return service.retrieveUserGroupDto(context.getUsername());
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Creates a user group with the group owner as the only group
     * member. The group owner is the authenticated user in the token
     * context.
     * 
     * @param securityContext
     * @param groupName
     *            the name of the group
     * @return if a new group created, HTTP response status 201
     *         Created, otherwise 204 No Content.
     */
    @PUT
    @Path("@{groupName}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createUpdateUserGroup (
            @Context SecurityContext securityContext,
            @PathParam("groupName") String groupName,
            @FormParam("description") String description) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.CREATE_USER_GROUP);
            boolean groupExists = service.createUpdateUserGroup(groupName,
                    description, context.getUsername());
            if (groupExists) {
                return Response.noContent().build();
            }
            else {
                return Response.status(HttpStatus.SC_CREATED).build();
            }
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Deletes a user-group specified by the group name. Only group
     * owner and system admins can delete groups.
     * 
     * @param securityContext
     * @param groupName
     *            the name of the group to delete
     * @return HTTP 200, if successful.
     */
    @DELETE
    @Path("@{groupName}")
    public Response deleteUserGroup (@Context SecurityContext securityContext,
            @PathParam("groupName") String groupName) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
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
     * Removes a user-group member. Group owner cannot be removed.
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
    @Path("@{groupName}/~{memberUsername}")
    public Response removeUserFromGroup (
            @Context SecurityContext securityContext,
            @PathParam("memberUsername") String memberUsername,
            @PathParam("groupName") String groupName) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
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
     * Invites users to join a user-group specified by the
     * groupName. Only user-group admins and system admins are
     * allowed to use this service.
     * 
     * The invited users are added as group members with status
     * GroupMemberStatus.PENDING.
     * 
     * If a user accepts the invitation by using the service:
     * {@link UserGroupController#subscribeToGroup(SecurityContext, String)},
     * his GroupMemberStatus will be updated to
     * GroupMemberStatus.ACTIVE.
     * 
     * If a user rejects the invitation by using the service:
     * {@link UserGroupController#unsubscribeFromGroup(SecurityContext, String)},
     * his GroupMemberStatus will be updated to
     * GroupMemberStatus.DELETED.
     * 
     * @param securityContext
     * @param members
     *            usernames separated by comma
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("@{groupName}/invite")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response inviteGroupMembers (
            @Context SecurityContext securityContext,
            @PathParam("groupName") String groupName,
            @FormParam("members") String members) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        try {
            scopeService.verifyScope(context,
                    OAuth2Scope.ADD_USER_GROUP_MEMBER);
            service.inviteGroupMembers(groupName, members,
                    context.getUsername());
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
     *            the group name
     * @param memberUsername
     *            the username of a group-member
     * @param roleId
     *            a role id or multiple role ids
     * @return
     */
    @POST
    @Path("@{groupName}/role/edit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response editMemberRoles (@Context SecurityContext securityContext,
            @PathParam("groupName") String groupName,
            @FormParam("memberUsername") String memberUsername,
            @FormParam("roleId") List<Integer> roleIds) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
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
     * @param roleId
     *            a role id or multiple role ids
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("@{groupName}/role/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addMemberRoles (@Context SecurityContext securityContext,
            @PathParam("groupName") String groupName,
            @FormParam("memberUsername") String memberUsername,
            @FormParam("roleId") List<Integer> roleIds) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
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
     * given roles. Only user-group admins and system admins are
     * allowed.
     * 
     * @param securityContext
     * @param groupName
     *            a group name
     * @param memberUsername
     *            a username of a group member
     * @param roleId
     *            a role id or multiple role ids
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("@{groupName}/role/delete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteMemberRoles (@Context SecurityContext securityContext,
            @PathParam("groupName") String groupName,
            @FormParam("memberUsername") String memberUsername,
            @FormParam("roleId") List<Integer> roleIds) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
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
    @Path("@{groupName}/subscribe")
    public Response subscribeToGroup (@Context SecurityContext securityContext,
            @PathParam("groupName") String groupName) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
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
     * Implemented identical to
     * {@link #removeUserFromGroup(SecurityContext, String, String)}.
     * 
     * @param securityContext
     * @param groupName
     * @return if successful, HTTP response status OK
     */
    @DELETE
    @Path("@{groupName}/unsubscribe")
    public Response unsubscribeFromGroup (
            @Context SecurityContext securityContext,
            @PathParam("groupName") String groupName) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
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
