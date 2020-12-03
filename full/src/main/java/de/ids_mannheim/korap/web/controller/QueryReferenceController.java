package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.dto.VirtualCorpusAccessDto;
import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.QueryReferenceService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.input.VirtualCorpusJson;

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
    private QueryReferenceService service;
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
     * @param vcCreator
     *            the username of the vc creator, must be the same
     *            as the authenticated username
     * @param vcName
     *           the vc name
     * @param vc a json object describing the VC
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
        @QueryParam("desc") String desc,
        String query) throws KustvaktException {

        TokenContext context =
            (TokenContext) securityContext.getUserPrincipal();

        if (!qCreator.equals(context.getUsername())) {
        };
        
        try {
            scopeService.verifyScope(context, OAuth2Scope.EDIT_VC);
            Status status = service.storeQuery(
                query,
                qName,
                desc,
                context.getUsername());
            return Response.status(status).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        };
        
    }

    /**
     * Returns the virtual corpus with the given name and creator.
     * 
     * @param securityContext
     * @param createdBy
     *            vc creator
     * @param vcName
     *            vc name
     * @return the virtual corpus with the given name and creator.
     */
/*
    @GET
    @Path("~{createdBy}/{vcName}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
        DemoUserFilter.class, PiwikFilter.class })
    public VirtualCorpusDto retrieveVCByName (
            @Context SecurityContext securityContext,
            @PathParam("createdBy") String createdBy,
            @PathParam("vcName") String vcName) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            return service.retrieveVCByName(context.getUsername(), vcName,
                    createdBy);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
*/
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

    
    // TODO: List all queries available to the logged in user
    // TODO: List all queries of a sepcific user
    // TODO: Some admin routes missing.
    // TODO: Some sharing routes missing
};
