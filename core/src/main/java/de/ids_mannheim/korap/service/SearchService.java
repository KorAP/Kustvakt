package de.ids_mannheim.korap.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.de.init.VCLoader;
import de.ids_mannheim.korap.authentication.AuthenticationManager;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.rewrite.RewriteHandler;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.ClientsHandler;
import de.ids_mannheim.korap.web.SearchKrill;

@Service
public class SearchService extends BasicService{

    private static final boolean DEBUG = false;

    private static Logger jlog = LogManager.getLogger(SearchService.class);

    @Autowired
    private KustvaktConfiguration config;
    @Autowired
    private VCLoader vcLoader;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RewriteHandler rewriteHandler;

    @Autowired
    private SearchKrill searchKrill;

    private ClientsHandler graphDBhandler;

    @PostConstruct
    private void doPostConstruct () {
        UriBuilder builder = UriBuilder.fromUri("http://10.0.10.13").port(9997);
        this.graphDBhandler = new ClientsHandler(builder.build());
    }

    @SuppressWarnings("unchecked")
    public String serializeQuery (String q, String ql, String v, String cq,
            Integer pageIndex, Integer startPage, Integer pageLength,
            String context, Boolean cutoff, boolean accessRewriteDisabled)
            throws KustvaktException {
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
        if (DEBUG) {
            if (user != null) {
                jlog.debug("Debug: user location=" + user.locationtoString()
                        + ", access=" + user.accesstoString());
            }
        }
        return user;
    }

    public String search (String jsonld, String username, HttpHeaders headers)
            throws KustvaktException {

        User user = createUser(username, headers);

        JsonNode node  = JsonUtils.readTree(jsonld);
        node = node.at("/meta/snippets");
        if (node !=null && node.asBoolean()){
            user.setCorpusAccess(CorpusAccess.ALL);
        }
        
        String query = this.rewriteHandler.processQuery(jsonld, user);
        // MH: todo: should be possible to add the meta part to
        // the query serialization
        // User user = controller.getUser(ctx.getUsername());
        // jsonld = this.processor.processQuery(jsonld, user);
        return searchKrill.search(query);
    }

    @SuppressWarnings("unchecked")
    public String search (String engine, String username, HttpHeaders headers,
            String q, String ql, String v, List<String> cqList, String fields,
            String pipes, Integer pageIndex, Integer pageInteger, String ctx,
            Integer pageLength, Boolean cutoff, boolean accessRewriteDisabled)
            throws KustvaktException {

        if (pageInteger != null && pageInteger < 1) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "page must start from 1", "page");
        }
        
        String[] pipeArray = null;
        if (pipes!=null && !pipes.isEmpty()){
            pipeArray = pipes.split(",");
        }
        
        KustvaktConfiguration.BACKENDS eng = this.config.chooseBackend(engine);
        User user = createUser(username, headers);
        CorpusAccess corpusAccess = user.getCorpusAccess();
        
        // EM: TODO: check if requested fields are public metadata. Currently 
        // it is not needed because all metadata are public.        
        if (accessRewriteDisabled){
            corpusAccess = CorpusAccess.ALL;
            user.setCorpusAccess(CorpusAccess.ALL);
        }
        
        QuerySerializer serializer = new QuerySerializer();
        serializer.setQuery(q, ql, v);
        String cq = combineMultipleCorpusQuery(cqList);
        if (cq != null) serializer.setCollection(cq);

        List<String> fieldList = convertFieldsToList(fields);
        handleNonPublicFields(fieldList, accessRewriteDisabled, serializer);
        
        MetaQueryBuilder meta = createMetaQuery(pageIndex, pageInteger, ctx,
                pageLength, cutoff, corpusAccess, fieldList, accessRewriteDisabled);
        serializer.setMeta(meta.raw());
        
        // There is an error in query processing
        // - either query, corpus or meta
        if (serializer.hasErrors()) {
            throw new KustvaktException(serializer.toJSON());
        }

        String query = serializer.toJSON();
        query = runPipes(query,pipeArray);
        
        query = this.rewriteHandler.processQuery(query, user);
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

    /**
     * Pipes are service URLs for modifying KoralQuery. A POST request
     * with Content-Type application/json will be sent for each pipe.
     * Kustvakt expects a KoralQuery in JSON format as the pipe response. 
     * 
     * @param query the original koral query
     * @param pipeArray the pipe service URLs
     * @param serializer the query serializer
     * @return a modified koral query
     * @throws KustvaktException 
     */
    private String runPipes (String query, String[] pipeArray) throws KustvaktException {
        if (pipeArray !=null){
            for (int i=0; i<pipeArray.length; i++){
                String pipeURL = pipeArray[i];
                try {
                    Client client = Client.create();
                    WebResource resource = client.resource(pipeURL);
                    ClientResponse response =
                            resource.type(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .post(ClientResponse.class, query);
                    if (response.getStatus() == HttpStatus.SC_OK) {
                        String entity = response.getEntity(String.class);
                        if (entity != null && !entity.isEmpty()) {
                            query = entity;
                        }
                    }
                    else {
                        query = handlePipeError(query, pipeURL,
                                response.getStatus() + " "
                                        + response.getStatusInfo().toString());
                    }
                }
                catch (Exception e) {
                    query = handlePipeError(query, pipeURL,
                            e.getMessage());
                }
            }
        }
        return query;
    }
    
    private String handlePipeError (String query, String url,
            String message) throws KustvaktException {
        jlog.error("Failed running the pipe at " + url + ". Message: "+ message);
       
        Notifications n = new Notifications();
        n.addWarning(StatusCodes.PIPE_FAILED,
                "Pipe failed", url, message);
        JsonNode warning = n.toJsonNode();
        
        ObjectNode node = (ObjectNode) JsonUtils.readTree(query);
        if (node.has("warnings")){
            warning = warning.at("/warnings/0");
            ArrayNode arrayNode = (ArrayNode) node.get("warnings");
            arrayNode.add(warning);
            node.set("warnings", arrayNode);
        }
        else{
            node.setAll((ObjectNode) warning);
        }
        
        return node.toString(); 
    }

    private void handleNonPublicFields (List<String> fieldList,
            boolean accessRewriteDisabled, QuerySerializer serializer) {
        List<String> nonPublicFields = new ArrayList<>(); 
        nonPublicFields.add("snippet");
        
        List<String> ignoredFields = new ArrayList<>();
        if (accessRewriteDisabled && !fieldList.isEmpty()) {
            for (String field : fieldList) {
                if (nonPublicFields.contains(field)) {
                    ignoredFields.add(field);
                }
            }
            if (!ignoredFields.isEmpty()) {
                serializer.addWarning(StatusCodes.NON_PUBLIC_FIELD_IGNORED,
                        "The requested non public fields are ignored",
                        ignoredFields);
            }
        }
    }
    
    private MetaQueryBuilder createMetaQuery (Integer pageIndex,
            Integer pageInteger, String ctx, Integer pageLength,
            Boolean cutoff, CorpusAccess corpusAccess, List<String> fieldList,
            boolean accessRewriteDisabled) {
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("startIndex", pageIndex);
        meta.addEntry("startPage", pageInteger);
        meta.setSpanContext(ctx);
        meta.addEntry("count", pageLength);
        // todo: what happened to cutoff?
        meta.addEntry("cutOff", cutoff);
        meta.addEntry("snippets", !accessRewriteDisabled);
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
        
        if (fieldList != null && !fieldList.isEmpty()){
            meta.addEntry("fields", fieldList);
        }
        return meta;
    }

    private List<String> convertFieldsToList (String fields) {
        if (fields != null && !fields.isEmpty()) {
            String[] fieldArray = fields.split(",");
            List<String> fieldList = new ArrayList<>(fieldArray.length);
            for (String field : fieldArray) {
                fieldList.add(field.trim());
            }
            return fieldList;
        }
        else {
            return new ArrayList<>();
        }
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

    private Pattern determineAvailabilityPattern (User user) {
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
        return p;
    }
    
    public String retrieveMatchInfo (String corpusId, String docId,
            String textId, String matchId, Set<String> foundries,
            String username, HttpHeaders headers, Set<String> layers,
            boolean spans, boolean sentenceExpansion,
            boolean highlights) throws KustvaktException {
        String matchid =
                searchKrill.getMatchId(corpusId, docId, textId, matchId);

        User user = createUser(username, headers);
        Pattern p = determineAvailabilityPattern(user);

        boolean match_only = foundries == null || foundries.isEmpty();
        String results;
//        try {
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
                        spans, highlights, sentenceExpansion, p);
            }
            else {
                results = searchKrill.getMatch(matchid, p);
            }
//        }
//        catch (Exception e) {
//            jlog.error("Exception in the MatchInfo service encountered!", e);
//            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
//                    e.getMessage());
//        }
        if (DEBUG){
            jlog.debug("MatchInfo results: " + results);
        }
        return results;
    }

    public String retrieveDocMetadata (String corpusId, String docId,
            String textId, String fields, String username, HttpHeaders headers)
            throws KustvaktException {
        List<String> fieldList = null;
        if (fields != null && !fields.isEmpty()){
            fieldList = convertFieldsToList(fields);
        }
        Pattern p = null;
        if (config.isMetadataRestricted()){
            User user = createUser(username, headers);
            p = determineAvailabilityPattern(user);
        }
        String textSigle = searchKrill.getTextSigle(corpusId, docId, textId);
        return searchKrill.getFields(textSigle, fieldList, p);
    }
    
    public String getCollocationBase (String query) throws KustvaktException {
        return graphDBhandler.getResponse("distCollo", "q", query);
    }
    
    public void closeIndexReader (String token, ServletContext context)
            throws KustvaktException {

        if (token != null && !token.isEmpty()
                && token.equals(context.getInitParameter("adminToken"))) {
            searchKrill.closeIndexReader();
            vcLoader.recachePredefinedVC();
        }
        else {
            throw new KustvaktException(StatusCodes.INCORRECT_ADMIN_TOKEN,
                    "Admin token is incorrect");
        }
    }
    
}
