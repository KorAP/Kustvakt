package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.dto.QueryDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.QueryService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.input.QueryJson;

/**
 * QueryReferenceController defines web APIs related to the
 * management of query references.
 *
 * This controller is based on VirtualCorpusController.
 *
 * @author diewald, margaretha
 *
 */
@Controller
@Path("{version}/query")
@ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
        BlockingFilter.class, PiwikFilter.class })
public class QueryReferenceController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;
    @Autowired
    private QueryService service;
    @Autowired
    private OAuth2ScopeService scopeService;

    /**
     * Creates a query reference according to the given Json.
     * The query reference creator must be the same as the
     * authenticated username.
     * 
     * TODO: In the future, this may also update a query.
     *
     * @param securityContext
     * @param qCreator
     *            the username of the vc creator, must be the same
     *            as the authenticated username
     * @param qName
     *           the vc name
     * @param query a json object describing the query and its properties
     * @return
     * @throws KustvaktException
     */
    @PUT
    @Path("/~{qCreator}/{qName}")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response createQuery (
        @Context SecurityContext securityContext,
        @PathParam("qCreator") String qCreator,
        @PathParam("qName") String qName,
        QueryJson query) throws KustvaktException {

        TokenContext context =
            (TokenContext) securityContext.getUserPrincipal();

        try {
            scopeService.verifyScope(context, OAuth2Scope.EDIT_VC);
            Status status = service.handlePutRequest(context.getUsername(),
                    qCreator, qName, query);
            return Response.status(status).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        
    }

    /**
     * Returns the query with the given name and creator.
     * 
     * @param securityContext
     * @param createdBy
     *            query creator
     * @param qName
     *            query name
     * @return the query with the given name and creator.
     */
    @GET
    @Path("~{createdBy}/{qName}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
        DemoUserFilter.class, PiwikFilter.class })
    public QueryDto retrieveQueryByName (
            @Context SecurityContext securityContext,
            @PathParam("createdBy") String createdBy,
            @PathParam("qName") String qName) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            return service.retrieveQueryByName(context.getUsername(), qName,
                    createdBy, QueryType.QUERY);
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
     * @param createdBy
     *            vc creator
     * @param vcName
     *            vc name
     * @return HTTP status 200, if successful
     */
    /*
    @DELETE
    @Path("~{createdBy}/{vcName}")
    public Response deleteVCByName (@Context SecurityContext securityContext,
            @PathParam("createdBy") String createdBy,
            @PathParam("vcName") String vcName) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.DELETE_VC);
            service.deleteVCByName(context.getUsername(), vcName, createdBy);
        }
        catch (KustvaktException e) {
        throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    };
    */

    
    /**
     * Lists all queries available to the authenticated user.
     *
     * System-admins can list available queries for a specific user by
     * specifiying the username parameter.
     * 
     * Normal users cannot list queries available for other users. 
     * Thus, username parameter is optional
     * and must be identical to the authenticated username.
     * 
     * @param securityContext
     * @param username
     *            a username (optional)
     * @return a list of queries
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<QueryDto> listAvailableQuery (
            @Context SecurityContext securityContext,
            @QueryParam("username") String username) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            List<QueryDto> dtos = service.listAvailableQueryForUser(
                    context.getUsername(), username, QueryType.QUERY);
            return dtos;
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
    
//    // TODO: List all queries of a sepcific user
//    /**
//     * Lists all queries created by a user. This list is only
//     * available to the owner of the queries. Users, except system-admins, 
//     * are not allowed to list queries created by other users. 
//     * 
//     * Thus, the path parameter "createdBy" must be the same as the
//     * authenticated username. 
//     * 
//     * @param securityContext
//     * @return a list of queries created by the user
//     *         in the security context.
//     */
//    @GET
//    @Path("~{createdBy}")
//    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
//    public List<VirtualCorpusDto> listUserVC (
//            @PathParam("createdBy") String createdBy,
//            @Context SecurityContext securityContext) {
//        TokenContext context =
//                (TokenContext) securityContext.getUserPrincipal();
//        try {
//            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
//            return service.listOwnerVC(context.getUsername(), createdBy,
//                    QueryType.QUERY);
//        }
//        catch (KustvaktException e) {
//            throw kustvaktResponseHandler.throwit(e);
//        }
//    }
    
    
    // TODO: Some admin routes missing.
    // TODO: Some sharing routes missing
};
