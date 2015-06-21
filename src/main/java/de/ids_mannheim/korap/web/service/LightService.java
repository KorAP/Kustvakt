package de.ids_mannheim.korap.web.service;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KorAPException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resource.RewriteProcessor;
import de.ids_mannheim.korap.utils.CollectionQueryBuilder;
import de.ids_mannheim.korap.utils.KorAPLogger;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.web.ClientsHandler;
import de.ids_mannheim.korap.web.SearchLucene;
import de.ids_mannheim.korap.web.TRACE;
import de.ids_mannheim.korap.web.utils.KustvaktResponseHandler;
import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author hanl
 * @date 29/01/2014
 */
//todd test functions
@Path("v0.1" + "/")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LightService {

    private static Logger jlog = KorAPLogger.initiate(LightService.class);

    private SearchLucene searchLucene;
    private ClientsHandler graphDBhandler;
    private RewriteProcessor processor;

    public LightService() {
        this.searchLucene = new SearchLucene(
                BeanConfiguration.getConfiguration().getIndexDir());
        UriBuilder builder = UriBuilder.fromUri("http://10.0.10.13").port(9997);
        this.graphDBhandler = new ClientsHandler(builder.build());
        this.processor = new RewriteProcessor();
    }

    @POST
    @Path("colloc")
    public Response getCollocationBase(@QueryParam("q") String query) {
        String result;
        try {
            result = graphDBhandler.getResponse("distCollo", "q", query);
        }catch (KorAPException e) {
            throw KustvaktResponseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }

    // todo
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
        if (pageIndex != null)
            meta.addEntry("startIndex", pageIndex);
        if (pageIndex == null && startPage != null)
            meta.addEntry("startPage", startPage);
        if (pageLength != null)
            meta.addEntry("count", pageLength);
        if (context != null)
            meta.setSpanContext(context);
        meta.addEntry("cutOff", cutoff);
        ss.addMeta(meta);
        //todo: test this
        ss.setCollection(cq);
        return Response.ok(processor.process(ss.toJSON())).build();
    }

    @POST
    @Path("search")
    public Response queryRaw(@QueryParam("engine") String engine,
            String jsonld) {
        KustvaktConfiguration.BACKENDS eng = BeanConfiguration
                .getConfiguration().chooseBackend(engine);
        jsonld = processor.process(jsonld);
        // todo: should be possible to add the meta part to the query serialization
        jlog.info("Serialized search: {}", jsonld);

        // fixme: to use the systemarchitecture pointcut thingis, searchlucene must be injected via
        String result = searchLucene.search(jsonld);
        KorAPLogger.QUERY_LOGGER.trace("The result set: {}", result);
        return Response.ok(result).build();
    }

    @GET
    @Path("search")
    public Response searchbyNameAll(@Context SecurityContext securityContext,
            @QueryParam("q") String q, @QueryParam("ql") String ql,
            @QueryParam("v") String v, @QueryParam("context") String ctx,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer pageInteger,
            @QueryParam("cq") String cq, @QueryParam("engine") String engine) {
        KustvaktConfiguration.BACKENDS eng = BeanConfiguration
                .getConfiguration().chooseBackend(engine);

        String result;
        QuerySerializer serializer = new QuerySerializer().setQuery(q, ql, v);
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.fillMeta(pageIndex, pageInteger, pageLength, ctx, cutoff);
        // fixme: should only apply to CQL queries per default!
        //        meta.addEntry("itemsPerResource", 1);
        serializer.addMeta(meta);

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
            }catch (KorAPException e) {
                throw KustvaktResponseHandler.throwit(e);
            }
        }else
            result = searchLucene.search(query);
        KorAPLogger.QUERY_LOGGER.trace("The result set: {}", result);
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
    //fixme: does not use policyrewrite!
    @GET
    @Path("/{type}/{id}/search")
    public Response searchbyName(@QueryParam("q") String query,
            @QueryParam("ql") String ql, @QueryParam("v") String v,
            @QueryParam("context") String ctx,
            @QueryParam("cutoff") Boolean cutoff,
            @QueryParam("count") Integer pageLength,
            @QueryParam("offset") Integer pageIndex,
            @QueryParam("page") Integer pageInteger, @PathParam("id") String id,
            @PathParam("type") String type, @QueryParam("cq") String cq,
            @QueryParam("raw") Boolean raw,
            @QueryParam("engine") String engine) {
        // ref is a virtual collection id!
        KustvaktConfiguration.BACKENDS eng = BeanConfiguration
                .getConfiguration().chooseBackend(engine);
        raw = raw == null ? false : raw;
        MetaQueryBuilder meta = new MetaQueryBuilder();
        if (!raw) {
            QuerySerializer s = new QuerySerializer().setQuery(query, ql, v);
            meta.fillMeta(pageIndex, pageInteger, pageLength, ctx, cutoff);
            // should only apply to CQL queries
            //                meta.addEntry("itemsPerResource", 1);
            s.addMeta(meta);
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
                result = searchLucene.search(query);

        }catch (Exception e) {
            KorAPLogger.ERROR_LOGGER
                    .error("Exception for serialized query: " + query, e);
            throw KustvaktResponseHandler.throwit(500, e.getMessage(), null);
        }

        KorAPLogger.QUERY_LOGGER.trace("The result set: {}", result);
        return Response.ok(result).build();
    }

    //todo: switch to new serialization
    @POST
    @Path("stats")
    public Response getStats(String json) {
        CollectionQueryBuilder builder = new CollectionQueryBuilder();
        builder.addResource(json);

        String stats = searchLucene.getStatistics(builder.toCollections());
        if (stats.contains("-1"))
            throw KustvaktResponseHandler.throwit(StatusCodes.EMPTY_RESULTS);

        return Response.ok(stats).build();
    }

    //fixme: only allowed for corpus?!
    @GET
    @Path("/corpus/{id}/{docid}/{rest}/matchInfo")
    public Response getMatchInfo(@PathParam("id") String id,
            @PathParam("docid") String docid, @PathParam("rest") String rest,
            @QueryParam("foundry") Set<String> foundries,
            @QueryParam("layer") Set<String> layers,
            @QueryParam("spans") Boolean spans) {
        spans = spans != null ? spans : false;
        String matchid = searchLucene.getMatchId(id, docid, rest);

        if (layers == null || layers.isEmpty())
            layers = new HashSet<>();

        boolean match_only = foundries == null || foundries.isEmpty();

        String results;
        // fixme: checks for policy matching
        // fixme: currently disabled, due to mishab in foundry/layer spec
        // fixme:
        if (foundries != null && foundries.size() > 1000) {
            Set<String> f_list = new HashSet<>();
            Set<String> l_list = new HashSet<>();

            for (String spl : new ArrayList<>(foundries)) {

                String[] sep = StringUtils.splitAnnotations(spl);
                if (spl != null) {
                    f_list.add(sep[0]);
                    l_list.add(sep[1]);
                }
                results = searchLucene
                        .getMatch(matchid, new ArrayList<>(f_list),
                                new ArrayList<>(l_list), spans, false, true);
            }
        }
        try {
            if (!match_only)
                results = searchLucene
                        .getMatch(matchid, new ArrayList<>(foundries),
                                new ArrayList<>(layers), spans, false, true);
            else
                results = searchLucene.getMatch(matchid);
        }catch (Exception e) {
            KorAPLogger.ERROR_LOGGER.error("Exception encountered!", e);
            throw KustvaktResponseHandler
                    .throwit(StatusCodes.ILLEGAL_ARGUMENT, e.getMessage(), "");
        }
        return Response.ok(results).build();
    }
}
