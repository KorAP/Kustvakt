package de.ids_mannheim.korap.web.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.SearchService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.utils.SearchResourceFilters;
import de.ids_mannheim.korap.web.utils.SearchResourceFiltersFeature;

/** Search and match info web-services
 *  
 *  The code is taken from {@link MainController}
 *  
 *  Resource filters for this class should be defined in a Kustvakt 
 *  configuration file. See {@link SearchResourceFilters} and 
 *  {@link SearchResourceFiltersFeature}
 * 
 * @author hanl, margaretha, diewald
 * 
 */

@Controller
@Path("{version}")
@SearchResourceFilters
public class SearchController {

    private static final boolean DEBUG = false;

    private static Logger jlog = LogManager.getLogger(SearchController.class);
    private @Context ServletContext context;
    
    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    @Autowired
    private SearchService searchService;
    @Autowired
    private OAuth2ScopeService scopeService;
    
    
    @POST
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response searchPost (@Context SecurityContext context,
            @Context Locale locale, 
            @Context HttpHeaders headers,
            String jsonld) {
        
        if (DEBUG){
            jlog.debug("Serialized search: " + jsonld);
        }
        
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        try {
            scopeService.verifyScope(ctx, OAuth2Scope.SEARCH);
            String result = searchService.search(jsonld, ctx.getUsername(),
                    headers);
            return Response.ok(result).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /** Performs for the given query 
     * 
     * @param securityContext
     * @param request
     * @param headers
     * @param locale
     * @param q
     *            query
     * @param ql
     *            query language
     * @param v
     *            query language version
     * @param ctx
     *            result context
     * @param cutoff
     *            determines to limit search results to one page only
     *            or not (default false)
     * @param pageLength
     *            the number of results should be included in a page
     * @param pageIndex 
     * @param pageInteger page number
     * @param fields
     *            metadata fields to be included, separated by comma
     * @param pipes
     *            external plugins for additional processing,
     *            separated by comma
     * @param accessRewriteDisabled
     *            determine if access rewrite should be disabled
     *            (default false)
     * @param cq
     *            corpus query defining a virtual corpus
     * @param engine
     * @return search results in JSON
     */
    @GET
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response searchGet (@Context SecurityContext securityContext,
            @Context HttpServletRequest request,
            @Context HttpHeaders headers, @Context Locale locale,
            @QueryParam("q") String q, @QueryParam("ql") String ql,
            @QueryParam("v") String v, @QueryParam("context") String ctx,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer pageInteger,
            @QueryParam("fields") String fields,
            @QueryParam("pipes") String pipes,
            @QueryParam("access-rewrite-disabled") boolean accessRewriteDisabled,
            @QueryParam("show-tokens") boolean showTokens,
            @QueryParam("cq") List<String> cq, 
            @QueryParam("engine") String engine) {

        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();

        String result;
        try {
            scopeService.verifyScope(context, OAuth2Scope.SEARCH);
            result = searchService.search(engine, context.getUsername(),
                    headers, q, ql, v, cq, fields, pipes, pageIndex,
                    pageInteger, ctx, pageLength, cutoff,
                    accessRewriteDisabled, showTokens);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

        return Response.ok(result).build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("corpus/{corpusId}/{docId}/{textId}/{matchId}")
    public Response retrieveMatchInfo (@Context SecurityContext ctx,
            @Context HttpHeaders headers, @Context Locale locale,
            @PathParam("corpusId") String corpusId,
            @PathParam("docId") String docId,
            @PathParam("textId") String textId,
            @PathParam("matchId") String matchId,
            @QueryParam("foundry") Set<String> foundries,
            @QueryParam("layer") Set<String> layers,
            @QueryParam("spans") Boolean spans, 
            @QueryParam("expand") String expansion, 
            // Highlights may also be a list of valid highlight classes
            @QueryParam("hls") Boolean highlights) throws KustvaktException {

        Boolean expandToSentence = true;
        if (expansion != null && (expansion.equals("false") || expansion.equals("null"))) {
            expandToSentence = false;
        }

        try {
            TokenContext tokenContext = (TokenContext) ctx.getUserPrincipal();
            scopeService.verifyScope(tokenContext, OAuth2Scope.MATCH_INFO);
            spans = spans != null ? spans : false;
            highlights = highlights != null ? highlights : false;
            if (layers == null || layers.isEmpty())
                layers = new HashSet<>();


            String results = searchService.retrieveMatchInfo(corpusId, docId,
                    textId, matchId, foundries, tokenContext.getUsername(),
                    headers, layers, spans, expandToSentence, highlights);
            return Response.ok(results).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

    }
    
    // EM: legacy support
//  @Deprecated
//  @GET
//  @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
//  @Path("/corpus/{corpusId}/{docId}/{textId}/{matchId}/matchInfo")
//  public Response getMatchInfo (@Context SecurityContext ctx,
//          @Context HttpHeaders headers, @Context Locale locale,
//          @PathParam("corpusId") String corpusId,
//          @PathParam("docId") String docId,
//          @PathParam("textId") String textId,
//          @PathParam("matchId") String matchId,
//          @QueryParam("foundry") Set<String> foundries,
//          @QueryParam("layer") Set<String> layers,
//          @QueryParam("spans") Boolean spans, 
//          // Highlights may also be a list of valid highlight classes
//          @QueryParam("hls") Boolean highlights) throws KustvaktException {
//
//      return retrieveMatchInfo(ctx, headers, locale, corpusId, docId, textId,
//                               matchId, foundries, layers, spans, "sentence", highlights);
//  }
}
