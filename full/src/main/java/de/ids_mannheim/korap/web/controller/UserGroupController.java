package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.service.UserGroupService;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.FullResponseHandler;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.input.UserGroupJson;

@Controller
@Path("group")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@ResourceFilters({ AuthenticationFilter.class, DemoUserFilter.class,
        PiwikFilter.class })
public class UserGroupController {

    private static Logger jlog =
            LoggerFactory.getLogger(UserGroupController.class);

    @Autowired
    private FullResponseHandler responseHandler;
    @Autowired
    private UserGroupService service;

    @GET
    @Path("list")
    public Response getUserGroup (@Context SecurityContext securityContext) {
        String result;
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            if (context.isDemo()) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Operation is not permitted for user: "
                                + context.getUsername(),
                        context.getUsername());
            }

            List<UserGroupDto> dtos =
                    service.retrieveUserGroup(context.getUsername());
            result = JsonUtils.toJSON(dtos);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }


    /** Creates a user group where the user in token context is the 
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
     * @param group UserGroupJson
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createGroup (@Context SecurityContext securityContext,
            UserGroupJson group) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            if (context.isDemo()) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Operation is not permitted for user: "
                                + context.getUsername(),
                        context.getUsername());
            }

            service.createUserGroup(group, context.getUsername());
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok().build();
    }


    @POST
    @Path("subscribe")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response subscribeToGroup (@Context SecurityContext securityContext,
            @FormParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            if (context.isDemo()) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Operation is not permitted for user: "
                                + context.getUsername(),
                        context.getUsername());
            }

            service.subscribe(groupId, context.getUsername());
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    @POST
    @Path("unsubscribe")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response unsubscribeFromGroup (
            @Context SecurityContext securityContext,
            @FormParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            if (context.isDemo()) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Operation is not permitted for user: "
                                + context.getUsername(),
                        context.getUsername());
            }

            service.unsubscribe(groupId, context.getUsername());
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok().build();
    }
}
