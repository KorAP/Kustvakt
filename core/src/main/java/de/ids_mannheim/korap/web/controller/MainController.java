package de.ids_mannheim.korap.web.controller;// package
                                             // de.ids_mannheim.korap.ext.web;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
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

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.SearchService;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AdminFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.utils.ResourceFilters;

/**
 * 
 * @author hanl, margaretha, diewald
 * @date 29/01/2014
 * @lastUpdate 05/07/2019
 * 
 */
@Controller
@Path("/")
@ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
        DemoUserFilter.class, PiwikFilter.class })
public class MainController {

    private static final boolean DEBUG = false;

    private static Logger jlog = LogManager.getLogger(MainController.class);
    private @Context ServletContext context;
    
    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    @Autowired
    private SearchService searchService;
    @Autowired
    private OAuth2ScopeService scopeService;
    @Autowired
    private KustvaktConfiguration config;
    
    @GET
    @Path("{version}")
    public Response index (){
        return Response
            .ok(config.getApiWelcomeMessage())
            .header("X-Index-Revision", searchService.getIndexFingerprint())
            .build();
    }
    
    @GET
    @Path("{version}/info")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response info (){
        Map<String, Object> m = new HashMap<>();
        m.put("latest_api_version", config.getCurrentVersion());
        m.put("supported_api_versions", config.getSupportedVersions());
        m.put("kustvakt_version", ServiceInfo.getInfo().getVersion());
        m.put("krill_version", searchService.getKrillVersion());
        m.put("koral_version", ServiceInfo.getInfo().getKoralVersion());
        try {
            return Response.ok(JsonUtils.toJSON(m)).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
    
    @POST
    @Path("{version}/index/close")
    // overrides the whole filters
    @ResourceFilters({APIVersionFilter.class,AdminFilter.class})
    public Response closeIndexReader (){
        try {
            searchService.closeIndexReader();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }
    
    
//     EM: This web service is DISABLED until there is a need for it.
//     ND: In case rewrite is supported, it could be used to check the authorization 
//         scope without searching etc. In case not, it helps to compare queries in 
//         different query languages.
//     MH: ref query parameter removed!
//    @GET
//    @Path("{version}/query")
//    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response serializeQuery (@Context Locale locale,
            @Context SecurityContext securityContext, @QueryParam("q") String q,
            @QueryParam("ql") String ql, @QueryParam("v") String v,
            @QueryParam("context") String context,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer startPage,
            @QueryParam("access-rewrite-disabled") boolean accessRewriteDisabled,
            @QueryParam("cq") String cq) {
        TokenContext ctx = (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(ctx, OAuth2Scope.SERIALIZE_QUERY);
            String result = searchService.serializeQuery(q, ql, v, cq,
                    pageIndex, startPage, pageLength, context, cutoff,
                    accessRewriteDisabled);
            if (DEBUG){
                jlog.debug("Query: " + result);
            }
            return Response.ok(result).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /*
     * Returns the meta data fields of a certain document
     */
    // This is currently identical to LiteService#getMeta(),
    // but may need auth code to work following policies
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("{version}/corpus/{corpusId}/{docId}/{textId}")
    public Response getMetadata (@PathParam("corpusId") String corpusId,
            @PathParam("docId") String docId,
            @PathParam("textId") String textId,
            @QueryParam("fields") String fields,
            @Context SecurityContext ctx,
            @Context HttpHeaders headers
    ) throws KustvaktException {
        TokenContext tokenContext = (TokenContext) ctx.getUserPrincipal();
        try {
            String results = searchService.retrieveDocMetadata(corpusId, docId,
                    textId, fields, tokenContext.getUsername(), headers);
            return Response.ok(results).build();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

//  EM: This web service requires Karang and is DISABLED.
//    @POST
//    @Path("{version}/colloc")
//    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
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
