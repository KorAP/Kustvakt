package de.ids_mannheim.korap.web.controller;// package
                                             // de.ids_mannheim.korap.ext.web;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.SearchService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

/**
 * 
 * @author hanl, margaretha, diewald
 * @date 29/01/2014
 * @lastUpdate 09/07/2018
 * 
 */
@Controller
@Path("/")
@ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
        DemoUserFilter.class, PiwikFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class SearchController {

    private static final boolean DEBUG = false;

    private static Logger jlog = LogManager.getLogger(SearchController.class);

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    @Autowired
    private SearchService searchService;
    @Autowired
    private OAuth2ScopeService scopeService;

    /**
     * Builds a json query serialization from the given parameters.
     * 
     * @param locale
     * @param securityContext
     * @param q
     *            query string
     * @param ql
     *            query language
     * @param v
     *            version
     * @param context
     * @param cutoff
     *            true if the number of results should be limited
     * @param pageLength
     *            number of results per page
     * @param pageIndex
     * @param startPage
     * @param cq
     *            corpus query
     * @return
     */
    // ref query parameter removed!
    @GET
    @Path("{version}/query")
    public Response serializeQuery (@Context Locale locale,
            @Context SecurityContext securityContext, @QueryParam("q") String q,
            @QueryParam("ql") String ql, @QueryParam("v") String v,
            @QueryParam("context") String context,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer startPage,
            @QueryParam("cq") String cq) {
        TokenContext ctx = (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(ctx, OAuth2Scope.SERIALIZE_QUERY);
            String result = searchService.serializeQuery(q, ql, v, cq,
                    pageIndex, startPage, pageLength, context, cutoff);
            if (DEBUG){
                jlog.debug("Query: " + result);
            }
            return Response.ok(result).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    @POST
    @Path("{version}/search")
    public Response searchPost (@Context SecurityContext context,
            @Context Locale locale, @QueryParam("engine") String engine,
            String jsonld) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();
        try {
            scopeService.verifyScope(ctx, OAuth2Scope.SEARCH);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

        if (DEBUG){
            jlog.debug("Serialized search: " + jsonld);
        }
        String result = searchService.search(jsonld);
        if (DEBUG){
            jlog.debug("The result set: " + result);
        }
        return Response.ok(result).build();
    }

    @GET
    @Path("{version}/search")
    public Response searchGet (@Context SecurityContext securityContext,
            @Context HttpHeaders headers, @Context Locale locale,
            @QueryParam("q") String q, @QueryParam("ql") String ql,
            @QueryParam("v") String v, @QueryParam("context") String ctx,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer pageInteger,
            @QueryParam("fields") String fields,
            @QueryParam("cq") String cq, @QueryParam("engine") String engine) {

        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();

        String result;
        try {
            scopeService.verifyScope(context, OAuth2Scope.SEARCH);
            result = searchService.search(engine, context.getUsername(),
                    headers, q, ql, v, cq, fields, pageIndex, pageInteger, ctx,
                    pageLength, cutoff);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

        return Response.ok(result).build();
    }

    @GET
    @Path("{version}/corpus/{corpusId}/{docId}/{textId}/{matchId}/matchInfo")
    public Response getMatchInfo (@Context SecurityContext ctx,
            @Context HttpHeaders headers, @Context Locale locale,
            @PathParam("corpusId") String corpusId,
            @PathParam("docId") String docId,
            @PathParam("textId") String textId,
            @PathParam("matchId") String matchId,
            @QueryParam("foundry") Set<String> foundries,
            @QueryParam("layer") Set<String> layers,
            @QueryParam("spans") Boolean spans, 
            // Highlights may also be a list of valid highlight classes
            @QueryParam("hls") Boolean highlights) throws KustvaktException {

        TokenContext tokenContext = (TokenContext) ctx.getUserPrincipal();
        scopeService.verifyScope(tokenContext, OAuth2Scope.MATCH_INFO);
        spans = spans != null ? spans : false;
        highlights = highlights != null ? highlights : false;
        if (layers == null || layers.isEmpty()) layers = new HashSet<>();

        String results = searchService.retrieveMatchInfo(corpusId, docId,
                textId, matchId, foundries, tokenContext.getUsername(), headers,
                layers, spans, highlights);
        return Response.ok(results).build();
    }

    /*
     * Returns the meta data fields of a certain document
     */
    // This is currently identical to LiteService#getMeta(),
    // but may need auth code to work following policies
    @GET
    @Path("{version}/corpus/{corpusId}/{docId}/{textId}")
    public Response getMetadata (@PathParam("corpusId") String corpusId,
            @PathParam("docId") String docId, @PathParam("textId") String textId
    // @QueryParam("fields") Set<String> fields
    ) throws KustvaktException {
        String results =
                searchService.retrieveDocMetadata(corpusId, docId, textId);
        return Response.ok(results).build();
    }

    @POST
    @Path("{version}/colloc")
    public Response getCollocationBase (@QueryParam("q") String query) {
        String result;
        try {
            result = searchService.getCollocationBase(query);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }

    // @GET
    // @Path("colloc")
    // public Response getCollocationsAll(@Context SecurityContext
    // ctx,
    // @Context Locale locale, @QueryParam("props") String properties,
    // @QueryParam("sfskip") Integer sfs,
    // @QueryParam("sflimit") Integer limit, @QueryParam("q") String
    // query,
    // @QueryParam("ql") String ql, @QueryParam("context") Integer
    // context,
    // @QueryParam("foundry") String foundry,
    // @QueryParam("paths") Boolean wPaths) {
    // TokenContext tokenContext = (TokenContext)
    // ctx.getUserPrincipal();
    // ColloQuery.ColloQueryBuilder builder;
    // KoralCollectionQueryBuilder cquery = new
    // KoralCollectionQueryBuilder();
    // String result;
    // try {
    // User user = controller.getUser(tokenContext.getUsername());
    // Set<VirtualCollection> resources = ResourceFinder
    // .search(user, VirtualCollection.class);
    // for (KustvaktResource c : resources)
    // cquery.addResource(((VirtualCollection) c).getQuery());
    //
    // builder = functions
    // .buildCollocations(query, ql, properties, context, limit,
    // sfs, foundry, new ArrayList<Dependency>(), wPaths,
    // cquery);
    //
    // result = graphDBhandler
    // .getResponse("distCollo", "q", builder.build().toJSON());
    // }catch (KustvaktException e) {
    // throw KustvaktResponseHandler.throwit(e);
    // }catch (JsonProcessingException e) {
    // throw
    // KustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT);
    // }
    // return Response.ok(result).build();
    // }

    // /**
    // * @param locale
    // * @param properties a json object string containing field, op
    // and value
    // for the query
    // * @param query
    // * @param context
    // * @return
    // */
    // @GET
    // @Path("{type}/{id}/colloc")
    // public Response getCollocations(@Context SecurityContext ctx,
    // @Context Locale locale, @QueryParam("props") String properties,
    // @QueryParam("sfskip") Integer sfs,
    // @QueryParam("sflimit") Integer limit, @QueryParam("q") String
    // query,
    // @QueryParam("ql") String ql, @QueryParam("context") Integer
    // context,
    // @QueryParam("foundry") String foundry,
    // @QueryParam("paths") Boolean wPaths, @PathParam("id") String
    // id,
    // @PathParam("type") String type) {
    // ColloQuery.ColloQueryBuilder builder;
    // type = StringUtils.normalize(type);
    // id = StringUtils.decodeHTML(id);
    // TokenContext tokenContext = (TokenContext)
    // ctx.getUserPrincipal();
    // String result;
    // try {
    // KoralCollectionQueryBuilder cquery = new
    // KoralCollectionQueryBuilder();
    // try {
    // User user = controller.getUser(tokenContext.getUsername());
    //
    // KustvaktResource resource = this.resourceHandler
    // .findbyStrId(id, user, type);
    //
    // if (resource instanceof VirtualCollection)
    // cquery.addResource(
    // ((VirtualCollection) resource).getQuery());
    // else if (resource instanceof Corpus)
    // cquery.addMetaFilter("corpusID",
    // resource.getPersistentID());
    // else
    // throw KustvaktResponseHandler
    // .throwit(StatusCodes.ILLEGAL_ARGUMENT,
    // "Type parameter not supported", type);
    //
    // }catch (KustvaktException e) {
    // throw KustvaktResponseHandler.throwit(e);
    // }catch (NumberFormatException ex) {
    // throw KustvaktResponseHandler
    // .throwit(StatusCodes.ILLEGAL_ARGUMENT);
    // }
    //
    // builder = functions
    // .buildCollocations(query, ql, properties, context, limit,
    // sfs, foundry, new ArrayList<Dependency>(), wPaths,
    // cquery);
    //
    // result = graphDBhandler
    // .getResponse("distCollo", "q", builder.build().toJSON());
    //
    // }catch (JsonProcessingException e) {
    // throw
    // KustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT);
    // }catch (KustvaktException e) {
    // throw KustvaktResponseHandler.throwit(e);
    // }
    //
    // return Response.ok(result).build();
    // }

}
