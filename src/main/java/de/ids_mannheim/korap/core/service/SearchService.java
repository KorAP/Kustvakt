package de.ids_mannheim.korap.core.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.config.KustvaktCacheable;
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
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;

@Service
public class SearchService extends BasicService {

    public class TotalResultCache extends KustvaktCacheable {

        public TotalResultCache () {
            super("total_results", "key:hashedKoralQuery");
        }
    }
    private static final boolean DEBUG = false;

    private static Logger jlog = LogManager.getLogger(SearchService.class);

    @Autowired
    private SearchNetworkEndpoint searchNetwork;

    private ClientsHandler graphDBhandler;

    private TotalResultCache totalResultCache;
    
    @Autowired
	protected RewriteHandler rewriteHandler;

    @Autowired
	protected KustvaktConfiguration config;
    
    @PostConstruct
    private void doPostConstruct () {
        UriBuilder builder = UriBuilder.fromUri("http://10.0.10.13").port(9997);
        this.graphDBhandler = new ClientsHandler(builder.build());

        totalResultCache = new TotalResultCache();
    }

    public String getKrillVersion () {
        return searchKrill.getIndex().getVersion();

    }

    @SuppressWarnings("unchecked")
    public String serializeQuery (String q, String ql, String v, String cq,
            Integer pageIndex, Integer startPage, Integer pageLength,
            String context, Boolean cutoff, boolean accessRewriteDisabled)
            throws KustvaktException {
        QuerySerializer ss = new QuerySerializer().setQuery(q, ql, v);
        if (cq != null)
            ss.setCollection(cq);

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

        ss.setMeta(meta.raw());
        // return ss.toJSON();

        String query = ss.toJSON();
        query = rewriteHandler.processQuery(ss.toJSON(), null);
        return query;
    }

    public String search (String jsonld, String username, HttpHeaders headers)
            throws KustvaktException {

        User user = createUser(username, headers);

        JsonNode node = JsonUtils.readTree(jsonld);
        node = node.at("/meta/snippets");
        if (node != null && node.asBoolean()) {
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
			String pipes, String responsePipes, Integer pageIndex,
			Integer pageInteger, String ctx, Integer pageLength, Boolean cutoff,
			boolean accessRewriteDisabled, boolean showTokens,
			boolean showSnippet) throws KustvaktException {

        if (pageInteger != null && pageInteger < 1) {
            throw new KustvaktException(StatusCodes.INVALID_ARGUMENT,
                    "page must start from 1", "page");
        }

        User user = createUser(username, headers);
        CorpusAccess corpusAccess = user.getCorpusAccess();

        // EM: TODO: check if requested fields are public metadata. Currently 
        // it is not needed because all metadata are public.        
        if (accessRewriteDisabled) {
            corpusAccess = CorpusAccess.ALL;
            user.setCorpusAccess(CorpusAccess.ALL);
        }

        QuerySerializer serializer = new QuerySerializer();
        serializer.setQuery(q, ql, v);
        String cq = combineMultipleCorpusQuery(cqList);
        if (cq != null)
            serializer.setCollection(cq);

        List<String> fieldList = convertFieldsToList(fields);
        handleNonPublicFields(fieldList, accessRewriteDisabled, serializer);

        MetaQueryBuilder meta = createMetaQuery(pageIndex, pageInteger, ctx,
                pageLength, cutoff, corpusAccess, fieldList,
                accessRewriteDisabled, showTokens, showSnippet);
        serializer.setMeta(meta.raw());

        // There is an error in query processing
        // - either query, corpus or meta
        if (serializer.hasErrors()) {
            throw new KustvaktException(serializer.toJSON());
        }

        String query = serializer.toJSON();

        if (accessRewriteDisabled && showTokens) {
            Notifications n = new Notifications();
            n.addWarning(StatusCodes.NOT_ALLOWED,
                    "Tokens cannot be shown without access.");
            JsonNode warning = n.toJsonNode();
            query = addWarning(query, warning);
        }

        // Query pipe rewrite
        query = runPipes(query, pipes);

        query = this.rewriteHandler.processQuery(query, user);
        if (DEBUG) {
            jlog.debug("the serialized query " + query);
        }

        int hashedKoralQuery = createTotalResultCacheKey(query);
        boolean hasCutOff = hasCutOff(query);
        if (config.isTotalResultCacheEnabled() && !hasCutOff) {
            query = precheckTotalResultCache(hashedKoralQuery, query);
        }

        KustvaktConfiguration.BACKENDS searchEngine = this.config
                .chooseBackend(engine);
        String result;
        if (searchEngine.equals(KustvaktConfiguration.BACKENDS.NEO4J)) {
            result = searchNeo4J(query, pageLength, meta, false);
        }
        else if (searchEngine.equals(KustvaktConfiguration.BACKENDS.NETWORK)) {
            result = searchNetwork.search(query);
        }
        else {
            result = searchKrill.search(query);
        }
        // jlog.debug("Query result: " + result);
        
        if (config.isTotalResultCacheEnabled()) {
            result = afterCheckTotalResultCache(hashedKoralQuery, result);
        }
        
        if (!hasCutOff) {
            result = removeCutOff(result);
        }
        
        // Response pipe rewrite
        result = runPipes(result, responsePipes);
        return result;

    }
    
    private String removeCutOff (String result) throws KustvaktException {
        ObjectNode resultNode = (ObjectNode) JsonUtils.readTree(result);
        ObjectNode meta = (ObjectNode) resultNode.at("/meta");
        meta.remove("cutOff");
        return resultNode.toString();
    }

    public int createTotalResultCacheKey (String query)
            throws KustvaktException {
        ObjectNode queryNode = (ObjectNode) JsonUtils.readTree(query);
        queryNode.remove("meta");
        return queryNode.toString().hashCode();
    }

    private String afterCheckTotalResultCache (int hashedKoralQuery,
            String result) throws KustvaktException {

        String totalResults = (String) totalResultCache
                .getCacheValue(hashedKoralQuery);
        if (totalResults != null) {
            ObjectNode queryNode = (ObjectNode) JsonUtils.readTree(result);
            ObjectNode meta = (ObjectNode) queryNode.at("/meta");
            if (meta.isMissingNode()) {
                queryNode.put("totalResults", Integer.valueOf(totalResults));
            }
            else {
                meta.put("totalResults", Integer.valueOf(totalResults));
            }
            result = queryNode.toString();
        }
        else {
            JsonNode node = JsonUtils.readTree(result);
            totalResults = node.at("/meta/totalResults").asText();
            boolean timeExceeded = node.at("/meta/timeExceeded").asBoolean();
            
            if (!timeExceeded && totalResults != null && !totalResults.isEmpty()
                    && Integer.parseInt(totalResults) > 0)
                totalResultCache.storeInCache(hashedKoralQuery, totalResults);
        }
        return result;
    }

    public String precheckTotalResultCache (int hashedKoralQuery, String query)
            throws KustvaktException {
        String totalResults = (String) totalResultCache
                .getCacheValue(hashedKoralQuery);
        if (totalResults != null) {
            // add cutoff
            ObjectNode queryNode = (ObjectNode) JsonUtils.readTree(query);
            ObjectNode meta = (ObjectNode) queryNode.at("/meta");
            meta.put("cutOff", "true");
            query = queryNode.toString();
        }
        return query;
    }

    private boolean hasCutOff (String query) throws KustvaktException {
        JsonNode queryNode = JsonUtils.readTree(query);
        JsonNode cutOff = queryNode.at("/meta/cutOff");
        if (cutOff.isMissingNode()) {
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Pipes are service URLs for modifying KoralQuery. A POST request
     * with Content-Type application/json will be sent for each pipe.
     * Kustvakt expects a KoralQuery in JSON format as the pipe
     * response.
     * 
     * @param query
     *            the original koral query
     * @param pipes
     *            the pipe service URLs
     * @param serializer
     *            the query serializer
     * @return a modified koral query
     * @throws KustvaktException
     */
    private String runPipes (String query, String pipes)
            throws KustvaktException {
    	if (pipes != null && !pipes.isEmpty()) {
			String[] pipeArray = pipes.split(",");
			
            for (int i = 0; i < pipeArray.length; i++) {
                String pipeURL = pipeArray[i];
                if (pipeURL.startsWith(config.getPipeHost())) {
                    try {
                        URL url = new URL(pipeURL);
                        HttpURLConnection connection = (HttpURLConnection) url
                                .openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type",
                                "application/json; charset=UTF-8");
                        connection.setRequestProperty("Accept", "application/json");
                        connection.setDoOutput(true);
                        OutputStream os = connection.getOutputStream();
                        byte[] input = query.getBytes("utf-8");
                        os.write(input, 0, input.length);
    
                        String entity = null;
                        if (connection.getResponseCode() == HttpStatus.SC_OK) {
                            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                            connection.getInputStream(), "utf-8"));
                            StringBuilder response = new StringBuilder();
                            String responseLine = null;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                            entity = response.toString();
                        }
    
                        if (entity != null && !entity.isEmpty()) {
                            query = entity;
                        }
                        else {
                            query = handlePipeError(query, pipeURL,
                                    connection.getResponseCode() + " "
                                            + connection.getResponseMessage());
                        }
                    }
                    catch (Exception e) {
                        query = handlePipeError(query, pipeURL, e.getMessage());
                    }
                }
                else {
					query = handlePipeError(query, pipeURL,
							"Unrecognized pipe URL");
                }
            }
    	}
        return query;
    }

    private String handlePipeError (String query, String url, String message)
            throws KustvaktException {
        jlog.warn(
                "Failed running the pipe at " + url + ". Message: " + message);

        Notifications n = new Notifications();
        n.addWarning(StatusCodes.PIPE_FAILED, "Pipe failed", url, message);
        JsonNode warning = n.toJsonNode();

        query = addWarning(query, warning);
        return query;
    }

    private String addWarning (String query, JsonNode warning)
            throws KustvaktException {
    	ObjectNode node = null;
		try {
			node = (ObjectNode) JsonUtils.readTree(query);
		}
		catch (Exception e) {
			throw new KustvaktException(StatusCodes.DESERIALIZATION_FAILED,
					"Invalid JSON format");
		}
        if (node.has("warnings")) {
            warning = warning.at("/warnings/0");
            ArrayNode arrayNode = (ArrayNode) node.get("warnings");
            arrayNode.add(warning);
            node.set("warnings", arrayNode);
        }
        else {
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
            Integer pageInteger, String ctx, Integer pageLength, Boolean cutoff,
            CorpusAccess corpusAccess, List<String> fieldList,
            boolean accessRewriteDisabled, boolean showTokens,
            boolean showSnippet) {
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("startIndex", pageIndex);
        meta.addEntry("startPage", pageInteger);
        meta.setSpanContext(ctx);
        meta.addEntry("count", pageLength);
        // todo: what happened to cutoff?
        meta.addEntry("cutOff", cutoff);
        meta.addEntry("snippets", (showSnippet && !accessRewriteDisabled));
        if (!accessRewriteDisabled) {
            meta.addEntry("tokens", showTokens);
        }

        // meta.addMeta(pageIndex, pageInteger, pageLength, ctx,
        // cutoff);
        // fixme: should only apply to CQL queries per default!
        // meta.addEntry("itemsPerResource", 1);

        if (corpusAccess.equals(CorpusAccess.FREE)) {
            meta.addEntry("timeout", 10000);
        }
        else {
            meta.addEntry("timeout", 90000);
        }

        if (fieldList != null && !fieldList.isEmpty()) {
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

        MultivaluedMap<String, String> map = new MultivaluedHashMap<String, String>();
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
            String textId, String matchId, boolean info, Set<String> foundries,
            String username, HttpHeaders headers, Set<String> layers,
            boolean spans, boolean snippet, boolean tokens,
            boolean sentenceExpansion, boolean highlights, boolean isDeprecated)
            throws KustvaktException {
        String matchid = searchKrill.getMatchId(corpusId, docId, textId,
                matchId);

        User user = createUser(username, headers);
        Pattern p = determineAvailabilityPattern(user);

        //        boolean match_only = foundries == null || foundries.isEmpty();
        String results;
        //        try {

        ArrayList<String> foundryList = null;
        ArrayList<String> layerList = null;

        if (foundries != null && !foundries.isEmpty()) {
            foundryList = new ArrayList<String>();
            layerList = new ArrayList<String>();
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
        }
        else {
            sentenceExpansion = false;
            spans = false;
            info = false;
            highlights = true;
        };

        results = searchKrill.getMatch(matchid, info, foundryList, layerList,
                spans, snippet, tokens, highlights, sentenceExpansion, p, isDeprecated);
        //        }
        //        catch (Exception e) {
        //            jlog.error("Exception in the MatchInfo service encountered!", e);
        //            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
        //                    e.getMessage());
        //        }
        if (DEBUG) {
            jlog.debug("MatchInfo results: " + results);
        }
        return results;
    }

    public String retrieveDocMetadata (String corpusId, String docId,
            String textId, String fields, String username, HttpHeaders headers)
            throws KustvaktException {
        List<String> fieldList = null;
        if (fields != null && !fields.isEmpty()) {
            fieldList = convertFieldsToList(fields);
        }
        Pattern p = null;
        if (config.isMetadataRestricted()) {
            User user = createUser(username, headers);
            p = determineAvailabilityPattern(user);
        }
        String textSigle = searchKrill.getTextSigle(corpusId, docId, textId);
        return searchKrill.getFields(textSigle, fieldList, p);
    }

    public String getCollocationBase (String query) throws KustvaktException {
        return graphDBhandler.getResponse("distCollo", "q", query);
    }

    public void closeIndexReader () throws KustvaktException {
        searchKrill.closeIndexReader();
    }

    /**
     * Return the fingerprint of the latest index revision.
     */
    public String getIndexFingerprint () {
        return searchKrill.getIndexFingerprint();
    }

    public TotalResultCache getTotalResultCache () {
        return totalResultCache;
    }
}
