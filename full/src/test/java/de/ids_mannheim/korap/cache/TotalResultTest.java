package de.ids_mannheim.korap.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.core.service.SearchService;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class TotalResultTest extends SpringJerseyTest {

    @Autowired
    private SearchService searchService;

    @Test
    public void testSearchWithPaging () throws KustvaktException {

        assertEquals(0, searchService.getTotalResultCache()
                .getAllCacheElements().size());

        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .queryParam("page", "1").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        int totalResults = node.at("/meta/totalResults").asInt();

        assertEquals(1, searchService.getTotalResultCache()
                .getAllCacheElements().size());

        response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .queryParam("page", "2").request().get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
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
                .queryParam("page", "2")
                .queryParam("cutoff", "true")
                .request().get();
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
                + "\"CC-BY.*\",\"key\":\"availability\",\"rewrites\":[{\"@type\":"
                + "\"koral:rewrite\",\"src\":\"Kustvakt\",\"operation\":\"operation:"
                + "insertion\",\"scope\":\"availability(FREE)\"}]}}";
        
        int cacheKey = searchService.createTotalResultCacheKey(query);
        assertEquals(null, searchService.getTotalResultCache()
                .getCacheValue(cacheKey));
        
        assertEquals(cacheSize,  searchService.getTotalResultCache()
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
}
