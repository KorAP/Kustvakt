package de.ids_mannheim.korap.web.service.light;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.QueryBuilderUtil;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resource.rewrite.RewriteHandler;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.web.ClientsHandler;
import de.ids_mannheim.korap.web.CoreResponseHandler;
import de.ids_mannheim.korap.web.SearchKrill;

/**
 * @author hanl
 * @date 29/01/2014
 * 
 * @author margaretha
 * @update 10/10/2017
 * 
 * <pre>
 * Recent changes:
 * - removed version from service paths
 * - altered service with path /search and method trace to path
 * /query and method get
 * </pre>
 */
@Controller
@Path("/")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LightService {

    private static Logger jlog = LoggerFactory.getLogger(LightService.class);

    @Autowired
    CoreResponseHandler kustvaktResponseHandler;
    @Autowired
    private SearchKrill searchKrill;
    private ClientsHandler graphDBhandler;
    @Autowired
    private RewriteHandler processor;
    @Autowired
    private KustvaktConfiguration config;


    public LightService () {
        // Karang URI
        UriBuilder builder = UriBuilder.fromUri("http://10.0.10.13").port(9997);
        this.graphDBhandler = new ClientsHandler(builder.build());
    }


    /** Requires Karang (Neo4j search engine)
     *  
     * @param query
     * @return response
     */
    @POST
    @Path("colloc")
    public Response getCollocationBase (@QueryParam("q") String query) {
        String result;
        try {
            result = graphDBhandler.getResponse("distCollo", "q", query);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }


    // todo
    @Deprecated
    public Response postMatchFavorite () {
        return Response.ok().build();
    }


    @GET
    @Path("query")
    public Response buildQuery (@QueryParam("q") String q,
            @QueryParam("ql") String ql, @QueryParam("v") String v,
            @QueryParam("context") String context,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer startPage,
            @QueryParam("cq") String cq) {
        QuerySerializer ss = new QuerySerializer().setQuery(q, ql, v);

        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("startIndex", pageIndex);
        if (pageIndex == null && startPage != null)
            meta.addEntry("startPage", startPage);
        meta.addEntry("count", pageLength);
        meta.setSpanContext(context);
        meta.addEntry("cutOff", cutoff);
        ss.setMeta(meta.raw());
        if (cq != null) ss.setCollection(cq);

        String query;
        try {
            query = this.processor.processQuery(ss.toJSON(), null);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok(query).build();
    }


    @POST
    @Path("search")
    public Response queryRaw (@QueryParam("engine") String engine,
            String jsonld) {
        try {
            jsonld = processor.processQuery(jsonld, null);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        // todo: should be possible to add the meta part to the query serialization
        jlog.info("Serialized search: {}", jsonld);
        try {
            String result = searchKrill.search(jsonld);
            jlog.debug("The result set: {}", result);
            return Response.ok(result).build();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }


    @GET
    @Path("search")
    public Response searchbyNameAll (@QueryParam("q") String q,
            @QueryParam("ql") String ql, @QueryParam("v") String v,
            @QueryParam("context") String ctx,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer pageInteger,
            @QueryParam("fields") Set<String> fields,
            // fixme: remove cq value from lightservice
            @QueryParam("cq") String cq, @QueryParam("engine") String engine) {
        KustvaktConfiguration.BACKENDS eng = this.config.chooseBackend(engine);

        String result;
        QuerySerializer serializer = new QuerySerializer().setQuery(q, ql, v);
        MetaQueryBuilder meta = QueryBuilderUtil.defaultMetaBuilder(pageIndex,
                pageInteger, pageLength, ctx, cutoff);
        if (fields != null && !fields.isEmpty())
            meta.addEntry("fields", fields);
        serializer.setMeta(meta.raw());
        if (cq != null) serializer.setCollection(cq);

        String query;
        try {
            query = this.processor.processQuery(serializer.toJSON(), null);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        jlog.info("the serialized query {}", query);

        // This may not work with the the KoralQuery
        if (eng.equals(KustvaktConfiguration.BACKENDS.NEO4J)) {
            MultivaluedMap map = new MultivaluedMapImpl();
            map.add("q", query);
            map.add("count", String.valueOf(pageLength));
            map.add("lctxs",
                    String.valueOf(meta.getSpanContext().getLeftSize()));
            map.add("rctxs",
                    String.valueOf(meta.getSpanContext().getRightSize()));

            try {
                result = this.graphDBhandler.getResponse(map, "distKwic");
            }
            catch (KustvaktException e) {
                throw kustvaktResponseHandler.throwit(e);
            }
        }
        else
            result = searchKrill.search(query);
        jlog.debug("The result set: {}", result);
        return Response.ok(result).build();
    }


    /**
     * param context will be like this: context: "3-t,2-c"
     * <p/>
     * id does not have to be an integer. name is also possible, in
     * which case a type reference is required
     * 
     * @param query
     * @param ql
     * @param v
     * @param ctx
     * @param cutoff
     * @param pageLength
     * @param pageIndex
     * @param pageInteger
     * @param id
     * @param type
     * @param cq
     * @param raw
     * @param engine
     * @return
     */
    //fixme: search in collection /collection/collection-id/search
    @Deprecated
    @GET
    //    @Path("/{type}/{id}/search")
    public Response searchbyName (@PathParam("id") String id,
            @PathParam("type") String type, @QueryParam("q") String query,
            @QueryParam("ql") String ql, @QueryParam("v") String v,
            @QueryParam("context") String ctx,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer pageInteger,
            @QueryParam("cq") String cq, @QueryParam("raw") Boolean raw,
            @QueryParam("engine") String engine) {
        // ref is a virtual collection id!
        KustvaktConfiguration.BACKENDS eng = this.config.chooseBackend(engine);
        raw = raw == null ? false : raw;
        MetaQueryBuilder meta = QueryBuilderUtil.defaultMetaBuilder(pageIndex,
                pageInteger, pageLength, ctx, cutoff);
        if (!raw) {
            // should only apply to CQL queries
            //                meta.addEntry("itemsPerResource", 1);
            QuerySerializer s = new QuerySerializer().setQuery(query, ql, v)
                    .setMeta(meta.raw());
            try {
                query = this.processor.processQuery(s.toJSON(), null);
            }
            catch (KustvaktException e) {
                throw kustvaktResponseHandler.throwit(e);
            }
        }
        String result;
        try {
            if (eng.equals(KustvaktConfiguration.BACKENDS.NEO4J)) {
                if (raw) throw kustvaktResponseHandler.throwit(
                        StatusCodes.ILLEGAL_ARGUMENT, "raw not supported!",
                        null);
                MultivaluedMap map = new MultivaluedMapImpl();
                map.add("q", query);
                map.add("count", String.valueOf(pageLength));
                map.add("lctxs",
                        String.valueOf(meta.getSpanContext().getLeftSize()));
                map.add("rctxs",
                        String.valueOf(meta.getSpanContext().getRightSize()));
                result = this.graphDBhandler.getResponse(map, "distKwic");
            }
            else
                result = searchKrill.search(query);

        }
        catch (Exception e) {
            jlog.error("Exception for serialized query: " + query, e);
            throw kustvaktResponseHandler.throwit(500, e.getMessage(), null);
        }

        jlog.debug("The result set: {}", result);
        return Response.ok(result).build();
    }


    @GET
    @Path("statistics")
    public Response getStatistics (
            @QueryParam("collectionQuery") String collectionQuery) {

        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
        builder.with(collectionQuery);
        String json;
        try {
            json = builder.toJSON();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

        String stats = searchKrill.getStatistics(json);
        if (stats.contains("-1"))
            throw kustvaktResponseHandler.throwit(StatusCodes.NO_RESULT_FOUND);
        jlog.debug("Stats: " + stats);
        return Response.ok(stats).build();
    }



    /*
     * TODO: The problem here is, that the matchinfo path makes no
     * distinction between docs and texts - unlike DeReKo, the backend
     * and the frontend. Luckily there is a convenient method
     * "getMatchID()" for a workaround, but this should be fixed.
     */
    @GET
    @Path("/corpus/{corpusId}/{docId}/{textId}/{matchId}/matchInfo")
    public Response getMatchInfo (@PathParam("corpusId") String corpusId,
            @PathParam("docId") String docId,
            @PathParam("textId") String textId,
            @PathParam("matchId") String matchId,
            @QueryParam("foundry") Set<String> foundries,
            @QueryParam("layer") Set<String> layers,
            @QueryParam("spans") Boolean spans,
            @Context HttpServletRequest request) throws KustvaktException {

        String matchid =
                searchKrill.getMatchId(corpusId, docId, textId, matchId);
        
        List<String> f_list = null;
        List<String> l_list = null;
        if (layers != null && !layers.isEmpty())
            l_list = new ArrayList<>(layers);

        if (foundries != null && !foundries.isEmpty()
                && !foundries.contains("*"))
            f_list = new ArrayList<>(foundries);

        spans = spans != null ? spans : false;

        boolean match_only = foundries == null || foundries.isEmpty();
        String results;
        if (match_only)
            results = searchKrill.getMatch(matchid, null);
        else
            results = searchKrill.getMatch(matchid, f_list, l_list, spans,
                    false, true, null);

        return Response.ok(results).build();
    }

}
