package de.ids_mannheim.korap.web.service.light;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.QueryBuilderUtil;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resource.RewriteProcessor;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import de.ids_mannheim.korap.web.ClientsHandler;
import de.ids_mannheim.korap.web.SearchKrill;
import de.ids_mannheim.korap.web.TRACE;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;
import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author hanl
 * @date 29/01/2014
 */
@Path("v0.1" + "/")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LightService {

    private static Logger jlog = KustvaktLogger.initiate(LightService.class);

    private SearchKrill searchKrill;
    private ClientsHandler graphDBhandler;
    private RewriteProcessor processor;
    private KustvaktConfiguration config;

    public LightService() {
        this.config = BeanConfiguration.getBeans().getConfiguration();
        this.searchKrill = new SearchKrill(config.getIndexDir());
        UriBuilder builder = UriBuilder.fromUri("http://10.0.10.13").port(9997);
        this.graphDBhandler = new ClientsHandler(builder.build());
        this.processor = new RewriteProcessor(this.config);
    }

    /**
     * @param query
     * @return response
     */
    @POST
    @Path("colloc")
    public Response getCollocationBase(@QueryParam("q") String query) {
        String result;
        try {
            result = graphDBhandler.getResponse("distCollo", "q", query);
        }catch (KustvaktException e) {
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }

    // todo
    @Deprecated
    public Response postMatchFavorite() {
        return Response.ok().build();
    }

    @TRACE
    @Path("search")
    public Response buildQuery(@QueryParam("q") String q,
            @QueryParam("ql") String ql, @QueryParam("v") String v,
            @QueryParam("context") String context,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer startPage,
            @QueryParam("ref") String reference, @QueryParam("cq") String cq) {
        QuerySerializer ss = new QuerySerializer().setQuery(q, ql, v);

        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("startIndex", pageIndex);
        if (pageIndex == null && startPage != null)
            meta.addEntry("startPage", startPage);
        meta.addEntry("count", pageLength);
        meta.setSpanContext(context);
        meta.addEntry("cutOff", cutoff);
        ss.setMeta(meta);
        if (cq != null)
            ss.setCollection(cq);
        return Response.ok(processor.process(ss.toJSON())).build();
    }

    @POST
    @Path("search")
    public Response queryRaw(@QueryParam("engine") String engine,
            String jsonld) {
        jsonld = processor.process(jsonld);
        // todo: should be possible to add the meta part to the query serialization
        jlog.info("Serialized search: {}", jsonld);

        // fixme: to use the systemarchitecture pointcut thingis, searchkrill must be injected via
        String result = searchKrill.search(jsonld);
        KustvaktLogger.QUERY_LOGGER.trace("The result set: {}", result);
        return Response.ok(result).build();
    }

    @GET
    @Path("search")
    public Response searchbyNameAll(@QueryParam("q") String q,
            @QueryParam("ql") String ql, @QueryParam("v") String v,
            @QueryParam("context") String ctx,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer pageInteger,
            @QueryParam("cq") String cq, @QueryParam("engine") String engine) {
        KustvaktConfiguration.BACKENDS eng = this.config.chooseBackend(engine);

        String result;
        QuerySerializer serializer = new QuerySerializer().setQuery(q, ql, v);
        MetaQueryBuilder meta = QueryBuilderUtil
                .defaultMetaBuilder(pageIndex, pageInteger, pageLength, ctx,
                        cutoff);
        serializer.setMeta(meta);
        // fixme: should only apply to CQL queries per default!
        //        meta.addEntry("itemsPerResource", 1);
        if (cq != null)
            serializer.setCollection(cq);

        String query = processor.process(serializer.toJSON());
        jlog.info("the serialized query {}", query);

        // This may not work with the the KoralQuery
        if (eng.equals(KustvaktConfiguration.BACKENDS.NEO4J)) {
            MultivaluedMap map = new MultivaluedMapImpl();
            map.add("q", query);
            map.add("count", String.valueOf(pageLength));
            map.add("lctxs",
                    String.valueOf(meta.getSpanContext().getLeft_size()));
            map.add("rctxs",
                    String.valueOf(meta.getSpanContext().getRight_size()));
            try {
                result = this.graphDBhandler.getResponse(map, "distKwic");
            }catch (KustvaktException e) {
                throw KustvaktResponseHandler.throwit(e);
            }
        }else
            result = searchKrill.search(query);
        KustvaktLogger.QUERY_LOGGER.trace("The result set: {}", result);
        return Response.ok(result).build();
    }

    /**
     * param context will be like this: context: "3-t,2-c"
     * <p/>
     * id does not have to be an integer. name is also possible, in which case a type reference is required
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
    @GET
    @Path("/{type}/{id}/search")
    public Response searchbyName(@PathParam("id") String id,
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
        MetaQueryBuilder meta = QueryBuilderUtil
                .defaultMetaBuilder(pageIndex, pageInteger, pageLength, ctx,
                        cutoff);
        if (!raw) {
            // should only apply to CQL queries
            //                meta.addEntry("itemsPerResource", 1);
            QuerySerializer s = new QuerySerializer().setQuery(query, ql, v)
                    .setMeta(meta);
            query = processor.process(s.toJSON());
        }
        String result;
        try {
            if (eng.equals(KustvaktConfiguration.BACKENDS.NEO4J)) {
                if (raw)
                    throw KustvaktResponseHandler
                            .throwit(StatusCodes.ILLEGAL_ARGUMENT,
                                    "raw not supported!", null);
                MultivaluedMap map = new MultivaluedMapImpl();
                map.add("q", query);
                map.add("count", String.valueOf(pageLength));
                map.add("lctxs",
                        String.valueOf(meta.getSpanContext().getLeft_size()));
                map.add("rctxs",
                        String.valueOf(meta.getSpanContext().getRight_size()));
                result = this.graphDBhandler.getResponse(map, "distKwic");
            }else
                result = searchKrill.search(query);

        }catch (Exception e) {
            KustvaktLogger.ERROR_LOGGER
                    .error("Exception for serialized query: " + query, e);
            throw KustvaktResponseHandler.throwit(500, e.getMessage(), null);
        }

        KustvaktLogger.QUERY_LOGGER.trace("The result set: {}", result);
        return Response.ok(result).build();
    }

    //todo: switch to new serialization
    @POST
    @Path("stats")
    public Response getStats(String json) {
        //        CollectionQueryBuilder builder = new CollectionQueryBuilder();
        //        builder.addResource(json);

        // todo: policy override in extension!
        String stats = searchKrill.getStatistics(json);
        if (stats.contains("-1"))
            throw KustvaktResponseHandler.throwit(StatusCodes.EMPTY_RESULTS);

        return Response.ok(stats).build();
    }

    @GET
    @Path("/corpus/{id}/{docid}/{rest}/matchInfo")
    public Response getMatchInfo(@PathParam("id") String id,
            @PathParam("docid") String docid, @PathParam("rest") String rest,
            @QueryParam("foundry") Set<String> foundries,
            @QueryParam("layer") Set<String> layers,
            @QueryParam("spans") Boolean spans) {
        String matchid = searchKrill.getMatchId(id, docid, rest);
        List<String> f_list = null;
        List<String> l_list = null;
        if (layers != null && !layers.isEmpty())
            l_list = new ArrayList<>(layers);

        if (foundries != null && !foundries.isEmpty() && !foundries
                .contains("*"))
            f_list = new ArrayList<>(foundries);

        boolean match_only = foundries == null || foundries.isEmpty();
        String results;
        if (match_only)
            results = searchKrill.getMatch(matchid);
        else
            results = searchKrill
                    .getMatch(matchid, f_list, l_list, spans, false, true);

        return Response.ok(results).build();
    }
}
