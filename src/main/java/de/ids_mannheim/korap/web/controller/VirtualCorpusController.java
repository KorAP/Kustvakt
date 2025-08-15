package de.ids_mannheim.korap.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.dto.QueryDto;
import de.ids_mannheim.korap.dto.RoleDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.QueryService;
import de.ids_mannheim.korap.utils.ParameterChecker;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIDeprecationFilter;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AdminFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.input.QueryJson;
import de.ids_mannheim.korap.web.utils.ResourceFilters;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

/**
 * VirtualCorpusController defines web APIs related to virtual corpus
 * (VC) such as creating, deleting and listing user virtual corpora.
 * All the APIs in this class are available to logged-in users, except
 * retrieving a VC info.
 * 
 * This class also includes web-services to share and publish VC. When
 * a VC is published, it is shared with all users, but not always listed 
 * like system VC. It is listed for a user, once when he/she have searched 
 * for the VC. A VC can be published by creating a new VC with type PUBLISHED
 * or editing an existing VC.
 * 
 * VC name must follow the following regex [a-zA-Z_0-9-.], other
 * characters are not allowed.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("{version}/vc")
@ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
        BlockingFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class VirtualCorpusController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;
    @Autowired
    private QueryService service;
    @Autowired
    private OAuth2ScopeService scopeService;

    /**
     * Creates a new VC with the given VC creator and VC name
     * specified as the path parameters. If a VC with the same name
     * and creator exists, the VC will be updated instead.
     * 
     * VC name cannot be updated.
     * 
     * The VC creator must be the same as the authenticated username,
     * except for system admins. System admins can create or update
     * system VC and any VC for any users.
     * 
     * 
     * @param securityContext
     * @param vcCreator
     *            the username of the vc creator, must be the same
     *            as the authenticated username, except admins
     * @param vcName
     *            the vc name
     * @param vc
     *            a json object describing the VC
     * @return HTTP Status 201 Created when creating a new VC, or 204
     *         No Content when updating an existing VC.
     * @throws KustvaktException
     */
    @PUT
    @Path("/~{vcCreator}/{vcName}")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response createUpdateVC (@Context SecurityContext securityContext,
    		@Context ContainerRequestContext requestContext,
            @PathParam("vcCreator") String vcCreator,
            @PathParam("vcName") String vcName, QueryJson vc)
            throws KustvaktException {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        
        List<PathSegment> pathSegments = requestContext.getUriInfo()
    			.getPathSegments();
        String version = pathSegments.get(0).getPath();
        double apiVersion = Double.parseDouble(version.substring(1));
        
        try {
            scopeService.verifyScope(context, OAuth2Scope.CREATE_VC);
            ParameterChecker.checkObjectValue(vc, "request entity");
            if (vc.getQueryType() == null) {
                vc.setQueryType(QueryType.VIRTUAL_CORPUS);
            }
            Status status = service.handlePutRequest(context.getUsername(),
                    vcCreator, vcName, vc, apiVersion);
            return Response.status(status).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

    }

    /**
     * Returns the virtual corpus with the given name and creator.
     * This web-service is also available for guests.
     * 
     * System admin can retrieve private or project vc of any users.
     * 
     * @param securityContext
     * @param createdBy
     *            vc creator
     * @param vcName
     *            vc name
     * @return the virtual corpus with the given name and creator.
     */
    @GET
    @Path("~{createdBy}/{vcName}")
    @ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
            DemoUserFilter.class})
    public QueryDto retrieveVCByName (@Context SecurityContext securityContext,
    		@Context ContainerRequestContext requestContext,
            @PathParam("createdBy") String createdBy,
            @PathParam("vcName") String vcName) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        List<PathSegment> pathSegments = requestContext.getUriInfo()
    			.getPathSegments();
        String version = pathSegments.get(0).getPath();
        double apiVersion = Double.parseDouble(version.substring(1));
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            return service.retrieveQueryByName(context.getUsername(), vcName,
                    createdBy, QueryType.VIRTUAL_CORPUS, apiVersion);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    @GET
    @Path("/koralQuery/~{createdBy}/{vcName}")
    public JsonNode retrieveVCKoralQuery (
            @Context SecurityContext securityContext,
            @PathParam("createdBy") String createdBy,
            @PathParam("vcName") String vcName) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            return service.retrieveKoralQuery(context.getUsername(), vcName,
                    createdBy, QueryType.VIRTUAL_CORPUS);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Retrieves field values of a virtual corpus, e.g. corpus sigle.
     * 
     * This service is restricted to system admin only.
     * 
     * @param securityContext
     * @param createdBy
     * @param vcName
     * @param fieldName
     * @return
     */
    @GET
    @Path("/field/~{createdBy}/{vcName}")
    @ResourceFilters({ APIVersionFilter.class, AdminFilter.class })
    public JsonNode retrieveVCField (@Context SecurityContext securityContext,
            @PathParam("createdBy") String createdBy,
            @PathParam("vcName") String vcName,
            @QueryParam("fieldName") String fieldName) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        try {
            return service.retrieveFieldValues(context.getUsername(), vcName,
                    createdBy, QueryType.VIRTUAL_CORPUS, fieldName);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Lists all virtual corpora (VC) available to the authenticated
     * user including PRIVATE VC created by the user, SYSTEM VC
     * defined by system-admins, and PROJECT VC available to
     * user-groups, wherein the user is a member of. The list can be
     * filtered to show only SYSTEM VC or VC owned by the user.
     * 
     * This web-service requires OAuth2 access token with scope:
     * vc_info.
     * 
     * @param securityContext
     * @param filter filter the list by system, own, or empty (default)
     * @return a list of virtual corpora
     */
    @GET
    public List<QueryDto> listAvailableVC (
    		@Context ContainerRequestContext requestContext,
            @Context SecurityContext securityContext,
            @QueryParam("filter-by") String filter) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        List<PathSegment> pathSegments = requestContext.getUriInfo()
    			.getPathSegments();
        String version = pathSegments.get(0).getPath();
        double apiVersion = Double.parseDouble(version.substring(1));

        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            if (filter != null && !filter.isEmpty()) {
                filter = filter.toLowerCase();
                if (filter.equals("system")) {
                    return service.listSystemQuery(QueryType.VIRTUAL_CORPUS, 
                    		apiVersion);
                }
                else if (filter.equals("own")) {
                    return service.listOwnerQuery(context.getUsername(),
                            QueryType.VIRTUAL_CORPUS, apiVersion);
                }
                else {
                    throw new KustvaktException(StatusCodes.UNSUPPORTED_VALUE,
                            "The given filter is unknown or not supported.");
                }
            }
            else {
                return service.listAvailableQueryForUser(context.getUsername(),
                        QueryType.VIRTUAL_CORPUS, apiVersion);
            }
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Lists all system virtual corpora, if PathParam
     * <em>createdBy</em> is specified to system or SYSTEM.
     * Otherwise, lists all virtual corpora created by the given user.
     * 
     * This web-service is only available to the owner of the vc.
     * Users, except system-admins, are not allowed to list vc created
     * by other users.
     * 
     * Beside "system or SYSTEM', the path parameter "createdBy" must
     * be the same as the
     * authenticated username.
     * 
     * @param createdBy
     *            system or username
     * @param securityContext
     * @return all system VC, if createdBy=system, otherwise a list of
     *         virtual corpora created by the authorized user.
     */
    @Deprecated
    @GET
    @Path("~{createdBy}")
    @ResourceFilters({APIDeprecationFilter.class, 
    	AuthenticationFilter.class, BlockingFilter.class})
    public List<QueryDto> listUserOrSystemVC (
            @PathParam("createdBy") String createdBy,
            @Context SecurityContext securityContext) {

        KustvaktException e = new KustvaktException(StatusCodes.DEPRECATED,
                "This service has been deprecated. Please use Virtual Corpus List "
                        + "web-service.");
        throw kustvaktResponseHandler.throwit(e);
    }

    /**
     * Group and system admins can delete VC. 
     * 
     * @param securityContext
     * @param createdBy
     *            vc creator
     * @param vcName
     *            vc name
     * @return HTTP status 200, if successful
     */
    @DELETE
    @Path("~{createdBy}/{vcName}")
    public Response deleteVCByName (@Context SecurityContext securityContext,
            @PathParam("createdBy") String createdBy,
            @PathParam("vcName") String vcName) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.DELETE_VC);
            service.deleteQueryByName(context.getUsername(), vcName, createdBy,
                    QueryType.VIRTUAL_CORPUS);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /**
     * VC can only be shared with a group, not individuals.
     * Only group admins are allowed to share VC and the VC must have
     * been created by themselves.
     * 
     * <br /><br />
     * Not allowed via third-party apps.
     * 
     * @param securityContext
     * @param vcCreator
     *            the username of the vc creator
     * @param vcName
     *            the name of the vc
     * @param groupName
     *            the name of the group to share
     * @return HTTP status 200, if successful
     */
    @POST
    @Path("~{vcCreator}/{vcName}/share/@{groupName}")
    public Response shareVC (@Context SecurityContext securityContext,
            @PathParam("vcCreator") String vcCreator,
            @PathParam("vcName") String vcName,
            @PathParam("groupName") String groupName) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.SHARE_VC);
            service.shareQuery(context.getUsername(), vcCreator, vcName,
                    groupName);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok("SUCCESS").build();
    }
    
    /**
     * Delete all roles for a given group name and vc. Only Group and
     * system admin are eligible.
     * 
     * @param securityContext
     * @param vcCreator
     * @param vcName
     * @param groupName
     * @return HTTP status 200, if successful
     */
    @DELETE
    @Path("~{vcCreator}/{vcName}/delete/@{groupName}")
    public Response deleteVC_AccessfromGroup (
            @Context SecurityContext securityContext,
            @PathParam("vcCreator") String vcCreator,
            @PathParam("vcName") String vcName,
            @PathParam("groupName") String groupName) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.DELETE_VC_ACCESS);
            service.deleteRoleByGroupAndQuery(groupName, vcCreator, vcName,
                    context.getUsername());
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /** DEPRECATED **
     * 
     * Only VCA Admins and system admins are allowed to delete a
     * VC-access.
     * 
     * <br /><br />
     * Not allowed via third-party apps.
     * 
     * @param securityContext
     * @param accessId
     * @return
     */
    @Deprecated
    @DELETE
    @Path("access/{accessId}")
    @ResourceFilters({APIDeprecationFilter.class, 
    	AuthenticationFilter.class, BlockingFilter.class})
    public Response deleteAccessById (
            @Context SecurityContext securityContext,
            @PathParam("accessId") int accessId) {
        throw kustvaktResponseHandler.throwit(new KustvaktException(
                StatusCodes.DEPRECATED,
                "This web-service is deprecated and will be completely removed "
                + "in API v1.1."));
    }
    
    /** This service may not be necessary thus it is hidden in the wiki.
     * 
     * Lists member roles in a group. It can be filtered by query, so only query
     * related roles should be listed.
     * 
     * Only available to group and system admins.
     * 
     * @param securityContext
     * @return a list of VC accesses
     */
    @GET
    @Path("access")
    public List<RoleDto> listRoles (@Context SecurityContext securityContext,
            @QueryParam("groupName") String groupName,
            @DefaultValue("true") @QueryParam("hasQuery") boolean hasQuery) {
        TokenContext context = (TokenContext) securityContext
                .getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_ACCESS_INFO);
            return service.listRolesByGroup(context.getUsername(), groupName,
                    hasQuery);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
}
