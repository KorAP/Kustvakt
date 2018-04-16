package de.ids_mannheim.korap.web.controller;// package
                                             // de.ids_mannheim.korap.ext.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.cache.ResourceCache;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration.BACKENDS;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.rewrite.FullRewriteHandler;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.web.ClientsHandler;
import de.ids_mannheim.korap.web.CoreResponseHandler;
import de.ids_mannheim.korap.web.SearchKrill;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

/**
 * 
 * @author hanl, margaretha
 * @date 29/01/2014
 * @lastUpdate 01/2018
 * 
 * removed deprecated codes
 */
@Controller
@Path("/")
@RequestMapping("/")
@ResourceFilters({ AuthenticationFilter.class, DemoUserFilter.class, PiwikFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class SearchController {

    private static Logger jlog =
            LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private CoreResponseHandler kustvaktExceptionHandler;
    @Autowired
    private SearchKrill searchKrill;
    private ResourceCache resourceHandler;
    @Autowired
    private AuthenticationManagerIface controller;
    private ClientsHandler graphDBhandler;
    @Autowired
    private FullConfiguration config;
    @Autowired
    private FullRewriteHandler processor;


    public SearchController () {
        this.resourceHandler = new ResourceCache();
        UriBuilder builder = UriBuilder.fromUri("http://10.0.10.13").port(9997);
        this.graphDBhandler = new ClientsHandler(builder.build());
    }

    @PostConstruct
    private void doPostConstruct () {
        this.processor.defaultRewriteConstraints();
    }

    // @GET
    // @Path("colloc")
    // public Response getCollocationsAll(@Context SecurityContext ctx,
    // @Context Locale locale, @QueryParam("props") String properties,
    // @QueryParam("sfskip") Integer sfs,
    // @QueryParam("sflimit") Integer limit, @QueryParam("q") String query,
    // @QueryParam("ql") String ql, @QueryParam("context") Integer context,
    // @QueryParam("foundry") String foundry,
    // @QueryParam("paths") Boolean wPaths) {
    // TokenContext tokenContext = (TokenContext) ctx.getUserPrincipal();
    // ColloQuery.ColloQueryBuilder builder;
    // KoralCollectionQueryBuilder cquery = new KoralCollectionQueryBuilder();
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
    // throw KustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT);
    // }
    // return Response.ok(result).build();
    // }


    // /**
    // * @param locale
    // * @param properties a json object string containing field, op and value
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
    // @QueryParam("sflimit") Integer limit, @QueryParam("q") String query,
    // @QueryParam("ql") String ql, @QueryParam("context") Integer context,
    // @QueryParam("foundry") String foundry,
    // @QueryParam("paths") Boolean wPaths, @PathParam("id") String id,
    // @PathParam("type") String type) {
    // ColloQuery.ColloQueryBuilder builder;
    // type = StringUtils.normalize(type);
    // id = StringUtils.decodeHTML(id);
    // TokenContext tokenContext = (TokenContext) ctx.getUserPrincipal();
    // String result;
    // try {
    // KoralCollectionQueryBuilder cquery = new KoralCollectionQueryBuilder();
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
    // throw KustvaktResponseHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT);
    // }catch (KustvaktException e) {
    // throw KustvaktResponseHandler.throwit(e);
    // }
    //
    // return Response.ok(result).build();
    // }
    @POST
    @Path("colloc")
    public Response getCollocationBase (@QueryParam("q") String query) {
        String result;
        try {
            result = graphDBhandler.getResponse("distCollo", "q", query);
        }
        catch (KustvaktException e) {
            throw kustvaktExceptionHandler.throwit(e);
        }
        return Response.ok(result).build();
    }


    /** Builds a json query serialization from the given parameters.
     * 
     * @param locale
     * @param securityContext
     * @param q query string
     * @param ql query language
     * @param v version
     * @param context
     * @param cutoff true if the number of results should be limited
     * @param pageLength number of results per page
     * @param pageIndex
     * @param startPage
     * @param cq collection query
     * @return
     */
    // ref query parameter removed!
    // EM: change the HTTP method to from TRACE to GET
    // EM: change path from search to query
    @GET
    @Path("query")
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
        QuerySerializer ss = new QuerySerializer().setQuery(q, ql, v);
        if (cq != null) ss.setCollection(cq);

        MetaQueryBuilder meta = new MetaQueryBuilder();
        if (pageIndex != null) meta.addEntry("startIndex", pageIndex);
        if (pageIndex == null && startPage != null)
            meta.addEntry("startPage", startPage);
        if (pageLength != null) meta.addEntry("count", pageLength);
        if (context != null) meta.setSpanContext(context);
        meta.addEntry("cutOff", cutoff);

        ss.setMeta(meta.raw());
        String result = ss.toJSON();
        jlog.debug("Query: " + result);
        return Response.ok(result).build();
    }


    /**
     * currently only supports either no reference at all in which
     * case all corpora are retrieved or a corpus name like "WPD".
     * No virtual collections supported!
     * 
     * @param locale
     * @param q
     * @param ql
     * @param v
     * @param pageLength
     * @param pageIndex
     * @return
     */

    // todo: does cq have any sensible worth here? --> would say no! --> is
    // useful in non type/id scenarios

    /* EM: potentially an unused service! */
    // EM: build query using the given virtual collection id
    // EM: change the HTTP method to from TRACE to GET
    // EM: change path from search to query
    // EM: there is no need to check resource licenses since the service just serialize a query serialization
    @GET
    @Path("{type}/{id}/query")
    public Response serializeQueryWithResource (@Context Locale locale,
            @Context SecurityContext securityContext, @QueryParam("q") String q,
            @QueryParam("ql") String ql, @QueryParam("v") String v,
            @QueryParam("context") String context,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer startPage,
            @PathParam("type") String type, @PathParam("id") String id) {
        TokenContext ctx = (TokenContext) securityContext.getUserPrincipal();
        type = StringUtils.normalize(type);
        id = StringUtils.decodeHTML(id);

        QuerySerializer ss = new QuerySerializer().setQuery(q, ql, v);

        MetaQueryBuilder meta = new MetaQueryBuilder();
        if (pageIndex != null) meta.addEntry("startIndex", pageIndex);
        if (pageIndex == null && startPage != null)
            meta.addEntry("startPage", startPage);
        if (pageLength != null) meta.addEntry("count", pageLength);
        if (context != null) meta.setSpanContext(context);
        if (cutoff != null) meta.addEntry("cutOff", cutoff);

        ss.setMeta(meta.raw());

        KoralCollectionQueryBuilder cquery = new KoralCollectionQueryBuilder();
        try {
            cquery.setBaseQuery(ss.toJSON());
        }
        catch (KustvaktException e1) {
            throw kustvaktExceptionHandler.throwit(e1);
        }

        String query = "";
        // EM: is this necessary at all?
        KustvaktResource resource = isCollectionIdValid(ctx.getName(), id);
        if (resource != null) {
            try {
                if (resource instanceof VirtualCollection) {
                    JsonNode node = cquery.and().mergeWith(resource.getData());
                    query = JsonUtils.toJSON(node);
                }
                else if (resource instanceof Corpus) {
                    cquery.and().with(Attributes.CORPUS_SIGLE, "=",
                            resource.getPersistentID());

                    query = cquery.toJSON();
                }
            }
            catch (KustvaktException e) {
                throw kustvaktExceptionHandler.throwit(e);
            }
        }

        jlog.debug("Query: " + query);
        return Response.ok(query).build();
    }

    // EM: prototype
    private KustvaktResource isCollectionIdValid (String username,
            String collectionId) {

        //        try {
        //            if (ctx.isDemo()) {
        //                // EM: FIX ME: Is there public VCs? set default username 
        // for nonlogin user, change demo? 
        //                Set set = ResourceFinder.searchPublicFiltered(
        //                        ResourceFactory.getResourceClass(type), id);
        //                resource = (KustvaktResource) set.toArray()[0];
        //            }
        //            else {
        //                // EM: FIX ME: search in user VC
        //                User user = controller.getUser(ctx.getUsername());
        //                if (StringUtils.isInteger(id))
        //                    resource = this.resourceHandler
        //                            .findbyIntId(Integer.valueOf(id), user);
        //                else
        //                    resource = this.resourceHandler.findbyStrId(id, user,
        //                            ResourceFactory.getResourceClass(type));
        //            }
        //        }
        //        // todo: instead of throwing exception, build notification and rewrites
        //        // into result query
        //        catch (KustvaktException e) {
        //            jlog.error("Exception encountered: {}", e.string());
        //            throw KustvaktResponseHandler.throwit(e);
        //        }

        return null;
    }


    @POST
    @Path("search")
    public Response queryRaw (@Context SecurityContext context,
            @Context Locale locale, @QueryParam("engine") String engine,
            String jsonld) {
        TokenContext ctx = (TokenContext) context.getUserPrincipal();

        // todo: should be possible to add the meta part to the query
        // serialization
        try {
            User user = controller.getUser(ctx.getUsername());
            // jsonld = this.processor.processQuery(jsonld, user);
        }
        catch (KustvaktException e) {
            throw kustvaktExceptionHandler.throwit(e);
        }
        jlog.info("Serialized search: {}", jsonld);

        String result = searchKrill.search(jsonld);
        // todo: logging
        KustvaktLogger.QUERY_LOGGER.trace("The result set: {}", result);
        return Response.ok(result).build();
    }


    @GET
    @Path("search")
    public Response search (@Context SecurityContext securityContext,
            @Context HttpHeaders headers, @Context Locale locale,
            @QueryParam("q") String q, @QueryParam("ql") String ql,
            @QueryParam("v") String v, @QueryParam("context") String ctx,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer pageInteger,
            @QueryParam("cq") String cq, @QueryParam("engine") String engine) {

        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        KustvaktConfiguration.BACKENDS eng = this.config.chooseBackend(engine);
        User user;
        try {
            user = controller.getUser(context.getUsername());
            controller.setAccessAndLocation(user, headers);
            //            System.out.printf("Debug: /search/: location=%s, access='%s'.\n", user.locationtoString(), user.accesstoString());
        }
        catch (KustvaktException e) {
            jlog.error("Failed retrieving user in the search service: {}",
                    e.string());
            throw kustvaktExceptionHandler.throwit(e);
        }

        QuerySerializer serializer = new QuerySerializer();
        serializer.setQuery(q, ql, v);
        if (cq != null) serializer.setCollection(cq);

        MetaQueryBuilder meta = createMetaQuery(pageIndex, pageInteger, ctx,
                pageLength, cutoff);
        serializer.setMeta(meta.raw());

        String query;
        try {
            query = this.processor.processQuery(serializer.toJSON(), user);
            jlog.info("the serialized query {}", query);
        }
        catch (KustvaktException e) {
            throw kustvaktExceptionHandler.throwit(e);
        }

        String result = doSearch(eng, query, pageLength, meta);
        jlog.debug("Query result: " + result);
        return Response.ok(result).build();
    }


    private MetaQueryBuilder createMetaQuery (Integer pageIndex,
            Integer pageInteger, String ctx, Integer pageLength,
            Boolean cutoff) {
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("startIndex", pageIndex);
        meta.addEntry("startPage", pageInteger);
        meta.setSpanContext(ctx);
        meta.addEntry("count", pageLength);
        // todo: what happened to cutoff?
        meta.addEntry("cutOff", cutoff);
        // meta.addMeta(pageIndex, pageInteger, pageLength, ctx, cutoff);
        // fixme: should only apply to CQL queries per default!
        // meta.addEntry("itemsPerResource", 1);
        return meta;
    }


    private String doSearch (BACKENDS eng, String query, Integer pageLength,
            MetaQueryBuilder meta) {
        String result;
        if (eng.equals(KustvaktConfiguration.BACKENDS.NEO4J)) {
            result = searchNeo4J(query, pageLength, meta, false);
        }
        else {
            result = searchKrill.search(query);
        }
        KustvaktLogger.QUERY_LOGGER.trace("The result set: {}", result);
        return result;

    }


    private String searchNeo4J (String query, int pageLength,
            MetaQueryBuilder meta, boolean raw) {

        if (raw) {
            throw kustvaktExceptionHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT,
                    "raw not supported!", null);
        }

        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.add("q", query);
        map.add("count", String.valueOf(pageLength));
        map.add("lctxs", String.valueOf(meta.getSpanContext().getLeftSize()));
        map.add("rctxs", String.valueOf(meta.getSpanContext().getRightSize()));
        try {
            return this.graphDBhandler.getResponse(map, "distKwic");
        }
        catch (KustvaktException e) {
            jlog.error("Failed searching in Neo4J: {}", e.string());
            throw kustvaktExceptionHandler.throwit(e);
        }

    }


    /**
     * @param context
     * @param locale
     * @param json
     * @return
     */
    // todo: rename
    @POST
    @Path("collection_raw")
    public Response createRawCollection (@Context SecurityContext context,
            @Context Locale locale, String json) {
        TokenContext c = (TokenContext) context.getUserPrincipal();
        VirtualCollection cache = ResourceFactory.getCachedCollection(json);
        User user;
        try {
            user = controller.getUser(c.getUsername());
        }
        catch (KustvaktException e) {
            jlog.error("Exception encountered: {}", e.string());
            throw kustvaktExceptionHandler.throwit(e);
        }

        VirtualCollection tmp = resourceHandler.getCache(cache.getId(),
                VirtualCollection.class);
        if (tmp == null) {
            String query;
            try {
                query = this.processor.processQuery(cache.getData(), user);
                String stats = searchKrill.getStatistics(query);
                cache.setStats(JsonUtils.convertToClass(stats, Map.class));
            }
            catch (KustvaktException e) {
                throw kustvaktExceptionHandler.throwit(e);
            }
            resourceHandler.cache(cache);
        }
        else
            cache = tmp;

        Map vals = new HashMap();
        vals.put("id", cache.getId());
        vals.put("statistics", cache.getStats());
        try {
            return Response.ok(JsonUtils.toJSON(vals)).build();
        }
        catch (KustvaktException e) {
            throw kustvaktExceptionHandler.throwit(e);
        }
    }


    @GET
    @Path("/corpus/{corpusId}/{docId}/{textId}/{matchId}/matchInfo")
    public Response getMatchInfo (@Context SecurityContext ctx,
            @Context HttpHeaders headers, @Context Locale locale,
            @PathParam("corpusId") String corpusId,
            @PathParam("docId") String docId,
            @PathParam("textId") String textId,
            @PathParam("matchId") String matchId,
            @QueryParam("foundry") Set<String> foundries,
            @QueryParam("layer") Set<String> layers,
            @QueryParam("spans") Boolean spans) throws KustvaktException {

        TokenContext tokenContext = (TokenContext) ctx.getUserPrincipal();
        spans = spans != null ? spans : false;

        String matchid =
                searchKrill.getMatchId(corpusId, docId, textId, matchId);
        if (layers == null || layers.isEmpty()) layers = new HashSet<>();

        boolean match_only = foundries == null || foundries.isEmpty();

        User user;
        try {
            user = controller.getUser(tokenContext.getUsername());
            controller.setAccessAndLocation(user, headers);
            System.out.printf(
                    "Debug: /getMatchInfo/: location=%s, access='%s'.\n",
                    user.locationtoString(), user.accesstoString());
        }
        catch (KustvaktException e) {
            jlog.error("Failed getting user in the matchInfo service: {}",
                    e.string());
            throw kustvaktExceptionHandler.throwit(e);
        }

        CorpusAccess corpusAccess = user.getCorpusAccess();
        Pattern p;
        switch (corpusAccess) {
            case PUB:
                p = config.getPublicLicensePattern();
                break;
            case ALL:
                p = config.getAllLicensePattern();
                break;
            default: // FREE
                p = config.getFreeLicensePattern();
                break;
        }

        String results;
        try {
            if (!match_only) {

                ArrayList<String> foundryList = new ArrayList<String>();
                ArrayList<String> layerList = new ArrayList<String>();

                // EM: now without user, just list all foundries and layers
                if (foundries.contains("*")) {
                    foundryList = config.getFoundries();
                    layerList = config.getLayers();
                }
                else {
                    foundryList.addAll(foundries);
                    layerList.addAll(layers);
                }

                results = searchKrill.getMatch(matchid, foundryList, layerList,
                        spans, false, true, p);
            }
            else {
                results = searchKrill.getMatch(matchid, p);
            }
        }
        catch (Exception e) {
            jlog.error("Exception in the MatchInfo service encountered!", e);
            throw kustvaktExceptionHandler.throwit(StatusCodes.ILLEGAL_ARGUMENT,
                    e.getMessage(), "");
        }
        jlog.debug("MatchInfo results: " + results);
        return Response.ok(results).build();
    }


	/*
     * Returns the meta data fields of a certain document
     */
	// This is currently identical to LiteService#getMeta(),
	// but may need auth code to work following policies
    @GET
    @Path("/corpus/{corpusId}/{docId}/{textId}")
    public Response getMeta (
		@PathParam("corpusId") String corpusId,
		@PathParam("docId") String docId,
		@PathParam("textId") String textId
		// @QueryParam("fields") Set<String> fields
		) throws KustvaktException {

		String textSigle = searchKrill.getTextSigle(corpusId, docId, textId);
		
		String results = searchKrill.getFields(textSigle);

        return Response.ok(results).build();
    }

}
