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

import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.dto.VirtualCorpusAccessDto;
import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Scope;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.VirtualCorpusService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.input.VirtualCorpusJson;

/**
 * VirtualCorpusController defines web APIs related to virtual corpus
 * (VC) such as creating, deleting and listing user virtual corpora.
 * 
 * This class also includes APIs related to virtual corpus access
 * (VCA) such as sharing and publishing VC. When a VC is published,
 * it is shared with all users, but not always listed like system
 * VC. It is listed for a user, once when he/she have searched for the
 * VC. A VC can be published by creating or editing the VC.
 * 
 * All the APIs in this class are available to logged-in users.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("vc")
@ResourceFilters({ AuthenticationFilter.class, BlockingFilter.class,
        PiwikFilter.class })
public class VirtualCorpusController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;
    @Autowired
    private VirtualCorpusService service;
    @Autowired
    private OAuth2ScopeService scopeService;

    /**
     * Creates a user virtual corpus, also for system admins
     * 
     * @see VirtualCorpusJson
     * 
     * @param securityContext
     * @param vc
     *            a JSON object describing the virtual corpus
     * @return HTTP Response OK if successful
     */
    @POST
    @Path("create")
    @Consumes("application/json")
    public Response createVC (@Context SecurityContext securityContext,
            VirtualCorpusJson vc) {
        try {
            // get user info
            TokenContext context =
                    (TokenContext) securityContext.getUserPrincipal();

            scopeService.verifyScope(context, OAuth2Scope.CREATE_VC);
            service.storeVC(vc, context.getUsername());
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /**
     * Edits a virtual corpus attributes including name, type and
     * corpus query. Only the virtual corpus owner and system admins
     * can edit a virtual corpus.
     * 
     * @see VirtualCorpusJson
     * 
     * @param securityContext
     * @param vc
     *            a JSON object describing the virtual corpus
     * @return HTTP Response OK if successful
     * @throws KustvaktException
     */
    @POST
    @Path("edit")
    @Consumes("application/json")
    public Response editVC (@Context SecurityContext securityContext,
            VirtualCorpusJson vc) throws KustvaktException {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();

        try {
            scopeService.verifyScope(context, OAuth2Scope.EDIT_VC);
            service.editVC(vc, context.getUsername());
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /**
     * Searches for a specific VC given the VC id.
     * 
     * @param securityContext
     * @param vcId
     *            a virtual corpus id
     * @return a list of virtual corpora
     */
    @GET
    @Path("{vcId}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public VirtualCorpusDto retrieveVC (
            @Context SecurityContext securityContext,
            @PathParam("vcId") int vcId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            return service.searchVCById(context.getUsername(), vcId);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Lists not only private virtual corpora but all virtual corpora
     * available to a user.
     * 
     * Users, except system admins, cannot list virtual corpora of
     * other users. Thus, createdBy parameter is only relevant for
     * requests from system admins.
     * 
     * @param securityContext
     * @param createdBy
     *            username of virtual corpus creator (optional)
     * @return a list of virtual corpora
     */
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<VirtualCorpusDto> listVCByUser (
            @Context SecurityContext securityContext,
            @QueryParam("createdBy") String createdBy) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            return service.listVCByUser(context.getUsername(), createdBy);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Lists all virtual corpora created by a user
     * 
     * @param securityContext
     * @return a list of virtual corpora created by the user
     *         in the security context.
     */
    @GET
    @Path("list/user")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<VirtualCorpusDto> listUserVC (
            @Context SecurityContext securityContext) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            return service.listOwnerVC(context.getUsername());
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Lists virtual corpora by creator and type. This is a controller
     * for system admin requiring valid system admin authentication.
     * 
     * If type is not specified, retrieves virtual corpora of all
     * types. If createdBy is not specified, retrieves virtual corpora
     * of all users.
     * 
     * @param securityContext
     * @param createdBy
     *            username of virtual corpus creator
     * @param type
     *            {@link VirtualCorpusType}
     * @return a list of virtual corpora
     */
    @GET
    @Path("list/system-admin")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<VirtualCorpusDto> listVCByStatus (
            @Context SecurityContext securityContext,
            @QueryParam("createdBy") String createdBy,
            @QueryParam("type") VirtualCorpusType type) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            if (context.getTokenType().equals(TokenType.BEARER)){
                throw new KustvaktException(
                        StatusCodes.AUTHENTICATION_FAILED,
                        "Token type Bearer is not allowed");   
            }
            return service.listVCByType(context.getUsername(), createdBy, type);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Only the VC owner and system admins can delete VC. VCA admins
     * can delete VC-accesses e.g. of project VC, but not the VC
     * themselves.
     * 
     * @param securityContext
     * @param vcId
     *            the id of the virtual corpus
     * @return HTTP status 200, if successful
     */
    @DELETE
    @Path("delete/{vcId}")
    public Response deleteVC (@Context SecurityContext securityContext,
            @PathParam("vcId") int vcId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.deleteVC(context.getUsername(), vcId);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /**
     * VC can only be shared with a group, not individuals.
     * Only VCA admins are allowed to share VC and the VC must have
     * been created by themselves.
     * 
     * <br /><br />
     * Not allowed via third-party apps.
     * 
     * @param securityContext
     * @param vcId
     *            a virtual corpus id
     * @param groupId
     *            a user group id
     * @return HTTP status 200, if successful
     */
    @POST
    @Path("access/share")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response shareVC (@Context SecurityContext securityContext,
            @FormParam("vcId") int vcId, @FormParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.shareVC(context.getUsername(), vcId, groupId);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /**
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
    @DELETE
    @Path("access/delete/{accessId}")
    public Response deleteVCAccess (@Context SecurityContext securityContext,
            @PathParam("accessId") int accessId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.deleteVCAccess(accessId, context.getUsername());
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }


    /**
     * Lists active VC accesses to the specified VC.
     * Only available to VCA and system admins.
     * For system admins, lists all VCA of the VC.
     * 
     * <br /><br />
     * Not allowed via third-party apps.
     * 
     * @see VirtualCorpusAccessStatus
     * 
     * @param securityContext
     * @param vcId
     *            virtual corpus id
     * @return a list of access to the specified virtual corpus
     */
    @GET
    @Path("access/list")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<VirtualCorpusAccessDto> listVCAccess (
            @Context SecurityContext securityContext,
            @QueryParam("vcId") int vcId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            return service.listVCAccessByVC(context.getUsername(), vcId);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Lists active VC-accesses available for a user-group.
     * Only available to VCA and system admins.
     * For system admins, list all VCA for the group.
     * 
     * <br /><br />
     * Not allowed via third-party apps.
     * 
     * @param securityContext
     * @param groupId
     *            a group id
     * @return a list of VC-access
     */
    @GET
    @Path("access/list/byGroup")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<VirtualCorpusAccessDto> listVCAccessByGroup (
            @Context SecurityContext securityContext,
            @QueryParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            return service.listVCAccessByGroup(context.getUsername(), groupId);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
}
