package de.ids_mannheim.korap.service;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.AuthenticationManager;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resource.rewrite.RewriteHandler;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.web.ClientsHandler;
import de.ids_mannheim.korap.web.SearchKrill;

@Service
public class SearchService {

    private static final boolean DEBUG = false;

    private static Logger jlog = LogManager.getLogger(SearchService.class);

    @Autowired
    private KustvaktConfiguration config;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RewriteHandler rewriteHandler;

    @Autowired
    private SearchKrill searchKrill;

    private ClientsHandler graphDBhandler;

    @PostConstruct
    private void doPostConstruct () {
        this.rewriteHandler.defaultRewriteConstraints();

        UriBuilder builder = UriBuilder.fromUri("http://10.0.10.13").port(9997);
        this.graphDBhandler = new ClientsHandler(builder.build());
    }

    @SuppressWarnings("unchecked")
    public String serializeQuery (String q, String ql, String v, String cq,
            Integer pageIndex, Integer startPage, Integer pageLength,
            String context, Boolean cutoff) throws KustvaktException {
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
        // return ss.toJSON();

        String query = ss.toJSON();
        query = rewriteHandler.processQuery(ss.toJSON(), null);
        return query;
    }

    private User createUser (String username, HttpHeaders headers)
            throws KustvaktException {
        User user = authenticationManager.getUser(username);
        authenticationManager.setAccessAndLocation(user, headers);
        if (user != null) {
            jlog.debug(
                    "Debug: /getMatchInfo/: location=" + user.locationtoString()
                            + ", access=" + user.accesstoString());
        }
        return user;
    }

    public String search (String jsonld) {
        // MH: todo: should be possible to add the meta part to
        // the query serialization
        // User user = controller.getUser(ctx.getUsername());
        // jsonld = this.processor.processQuery(jsonld, user);
        return searchKrill.search(jsonld);
    }

    @SuppressWarnings("unchecked")
    public String search (String engine, String username, HttpHeaders headers,
            String q, String ql, String v, String cq, Set<String> fields,
            Integer pageIndex, Integer pageInteger, String ctx,
            Integer pageLength, Boolean cutoff) throws KustvaktException {

        KustvaktConfiguration.BACKENDS eng = this.config.chooseBackend(engine);
        User user = createUser(username, headers);
        CorpusAccess corpusAccess = user.getCorpusAccess();
        
        QuerySerializer serializer = new QuerySerializer();
        serializer.setQuery(q, ql, v);
        if (cq != null) serializer.setCollection(cq);

        MetaQueryBuilder meta = createMetaQuery(pageIndex, pageInteger, ctx,
                pageLength, cutoff, corpusAccess);
        if (fields != null && !fields.isEmpty())
            meta.addEntry("fields", fields);
        serializer.setMeta(meta.raw());

        // There is an error in query processing
        // - either query, corpus or meta
        if (serializer.hasErrors()) {
            throw new KustvaktException(serializer.toJSON());
        }

        String query =
                this.rewriteHandler.processQuery(serializer.toJSON(), user);
        if (DEBUG){
            jlog.debug("the serialized query " + query);
        }

        String result;
        if (eng.equals(KustvaktConfiguration.BACKENDS.NEO4J)) {
            result = searchNeo4J(query, pageLength, meta, false);
        }
        else {
            result = searchKrill.search(query);
        }
        // jlog.debug("Query result: " + result);
        return result;

    }

    private MetaQueryBuilder createMetaQuery (Integer pageIndex,
            Integer pageInteger, String ctx, Integer pageLength,
            Boolean cutoff, CorpusAccess corpusAccess) {
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("startIndex", pageIndex);
        meta.addEntry("startPage", pageInteger);
        meta.setSpanContext(ctx);
        meta.addEntry("count", pageLength);
        // todo: what happened to cutoff?
        meta.addEntry("cutOff", cutoff);
        // meta.addMeta(pageIndex, pageInteger, pageLength, ctx,
        // cutoff);
        // fixme: should only apply to CQL queries per default!
        // meta.addEntry("itemsPerResource", 1);
        
        if (corpusAccess.equals(CorpusAccess.FREE)){
            meta.addEntry("timeout", 10000);
        }
        else{
            meta.addEntry("timeout", 90000);
        }
        return meta;
    }

    private String searchNeo4J (String query, int pageLength,
            MetaQueryBuilder meta, boolean raw) throws KustvaktException {

        if (raw) {
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "raw not supported!");
        }

        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.add("q", query);
        map.add("count", String.valueOf(pageLength));
        map.add("lctxs", String.valueOf(meta.getSpanContext().getLeftSize()));
        map.add("rctxs", String.valueOf(meta.getSpanContext().getRightSize()));
        return this.graphDBhandler.getResponse(map, "distKwic");

    }

    public String retrieveMatchInfo (String corpusId, String docId,
            String textId, String matchId, Set<String> foundries,
            String username, HttpHeaders headers, Set<String> layers,
            boolean spans, boolean highlights) throws KustvaktException {
        String matchid =
                searchKrill.getMatchId(corpusId, docId, textId, matchId);

        User user = createUser(username, headers);
        Pattern p = null;
        if (user != null) {
            CorpusAccess corpusAccess = user.getCorpusAccess();
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
        }
        boolean match_only = foundries == null || foundries.isEmpty();
        String results;
        try {
            if (!match_only) {

                ArrayList<String> foundryList = new ArrayList<String>();
                ArrayList<String> layerList = new ArrayList<String>();

                // EM: now without user, just list all foundries and
                // layers
                if (foundries.contains("*")) {
                    foundryList = config.getFoundries();
                    layerList = config.getLayers();
                }
                else {
                    foundryList.addAll(foundries);
                    layerList.addAll(layers);
                }

                results = searchKrill.getMatch(matchid, foundryList, layerList,
                        spans, highlights, true, p);
            }
            else {
                results = searchKrill.getMatch(matchid, p);
            }
        }
        catch (Exception e) {
            jlog.error("Exception in the MatchInfo service encountered!", e);
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    e.getMessage());
        }
        jlog.debug("MatchInfo results: " + results);
        return results;
    }

    public String retrieveDocMetadata (String corpusId, String docId,
            String textId) {
        String textSigle = searchKrill.getTextSigle(corpusId, docId, textId);
        return searchKrill.getFields(textSigle);
    }

    public String getCollocationBase (String query) throws KustvaktException {
        return graphDBhandler.getResponse("distCollo", "q", query);
    }
}
