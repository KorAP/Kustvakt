package de.ids_mannheim.korap.cache;

import static org.junit.Assert.assertEquals;

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
    }

}
