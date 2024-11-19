package de.ids_mannheim.korap.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.core.service.SearchService;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class TotalResultTest extends SpringJerseyTest {

    @Autowired
    private SearchService searchService;
    
    @Autowired
    private KustvaktConfiguration config;

    @Test
    public void testClearCache () {
        for (int i = 1; i < 10; i++) {
            searchService.getTotalResultCache().storeInCache(i, "10");
        }

        searchService.getTotalResultCache().clearCache();

        assertEquals(0, searchService.getTotalResultCache()
                .getAllCacheElements().size());
    }

    @Test
    public void testSearchWithPaging () throws KustvaktException {
        searchService.getTotalResultCache().clearCache();

        assertEquals(0, searchService.getTotalResultCache()
                .getAllCacheElements().size());

        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .queryParam("page", "1").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertTrue(node.at("/meta/totalResults").isNumber(),
                "totalResults should be a number");
        int totalResults = node.at("/meta/totalResults").asInt();
        assertEquals(1, searchService.getTotalResultCache()
                .getAllCacheElements().size());
        response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .queryParam("page", "2").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertTrue(node.at("/meta/totalResults").isNumber());
        assertEquals(totalResults, node.at("/meta/totalResults").asInt());
        assertEquals(1, searchService.getTotalResultCache()
                .getAllCacheElements().size());
        assertTrue(node.at("/meta/cutOff").isMissingNode());
        testSearchWithCutOff();
    }

    @Test
    public void testSearchWithCutOffTrue () throws KustvaktException {
        int cacheSize = searchService.getTotalResultCache()
                .getAllCacheElements().size();
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "ich").queryParam("ql", "poliqarp")
                .queryParam("page", "2").queryParam("cutoff", "true").request()
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String query = "{\"meta\":{\"startPage\":2,\"tokens\":false,\"cutOff\":"
                + "true,\"snippets\":true,\"timeout\":10000},\"query\":{\"@type\":"
                + "\"koral:token\",\"wrap\":{\"@type\":\"koral:term\",\"match\":"
                + "\"match:eq\",\"layer\":\"orth\",\"key\":\"ich\",\"foundry\":"
                + "\"opennlp\",\"rewrites\":[{\"@type\":\"koral:rewrite\",\"src\":"
                + "\"Kustvakt\",\"operation\":\"operation:injection\",\"scope\":"
                + "\"foundry\"}]}},\"@context\":\"http://korap.ids-mannheim.de/ns"
                + "/koral/0.3/context.jsonld\",\"collection\":{\"@type\":\"koral:"
                + "doc\",\"match\":\"match:eq\",\"type\":\"type:regex\",\"value\":"
                + "\"CC.*\",\"key\":\"availability\",\"rewrites\":[{\"@type\":"
                + "\"koral:rewrite\",\"src\":\"Kustvakt\",\"operation\":\"operation:"
                + "insertion\",\"scope\":\"availability(FREE)\"}]}}";
        int cacheKey = searchService.createTotalResultCacheKey(query);
        assertEquals(null,
                searchService.getTotalResultCache().getCacheValue(cacheKey));
        assertEquals(cacheSize, searchService.getTotalResultCache()
                .getAllCacheElements().size());
    }

    private void testSearchWithCutOff () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .queryParam("page", "3").queryParam("cutoff", "false").request()
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertTrue(node.at("/meta/cutOff").isMissingNode());
        response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .queryParam("page", "4").queryParam("cutoff", "true").request()
                .get();
        entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertTrue(node.at("/meta/cutOff").asBoolean());
    }
    
    @Test
    public void testCacheDisabled () throws KustvaktException {
        searchService.getTotalResultCache().clearCache();
        assertEquals(0, searchService.getTotalResultCache()
                .getAllCacheElements().size());

        config.setTotalResultCacheEnabled(false);
        
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=zu]").queryParam("ql", "poliqarp")
                .queryParam("page", "1").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertTrue(node.at("/meta/totalResults").isNumber(),
                "totalResults should be a number");
        assertEquals(0, searchService.getTotalResultCache()
                .getAllCacheElements().size());
        
        config.setTotalResultCacheEnabled(true);
        
        response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=zu]").queryParam("ql", "poliqarp")
                .queryParam("page", "1").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        assertEquals(1, searchService.getTotalResultCache()
                .getAllCacheElements().size());
        
        searchService.getTotalResultCache().clearCache();
    }
    
    @Test
    public void testCacheKey () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=populistischer]")
                .queryParam("ql", "poliqarp")
                .queryParam("cq", "availability!=QAO-NC-LOC:ids & corpusSigle = "
                        + "/SOL|[UTSZ][0-9][0-9]/ & pubDate in 1976")
                //.queryParam("fields", "corpusSigle,textSigle,pubDate,pubPlace,"
                //        + "availability,textClass")
                .queryParam("access-rewrite-disabled", "true")
                .queryParam("page", "1").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        
        ObjectNode queryNode = (ObjectNode) JsonUtils.readTree(entity);
        queryNode.remove("meta");
        queryNode.remove("matches");
//        int queryHashCode1 = queryNode.hashCode();
        int queryStringHashCode1 = queryNode.toString().hashCode();

        response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=populistisches]")
                .queryParam("ql", "poliqarp")
                .queryParam("cq", "availability!=QAO-NC-LOC:ids & corpusSigle = "
                        + "/SOL|[UTSZ][0-9][0-9]/ & pubDate in 1975")
                //.queryParam("fields", "corpusSigle,textSigle,pubDate,pubPlace,"
                //        + "availability,textClass")
                .queryParam("access-rewrite-disabled", "true")
                .queryParam("page", "1").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        entity = response.readEntity(String.class);
        
        queryNode = (ObjectNode) JsonUtils.readTree(entity);
        queryNode.remove("meta");
        queryNode.remove("matches");
//        int queryHashCode2 = queryNode.hashCode();
        int queryStringHashCode2 = queryNode.toString().hashCode();
        
//        assertEquals(queryHashCode1, queryHashCode2);
        assertNotEquals(queryStringHashCode1, queryStringHashCode2);
    }
}
