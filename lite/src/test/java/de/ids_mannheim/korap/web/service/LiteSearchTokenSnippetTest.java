package de.ids_mannheim.korap.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.ids_mannheim.korap.config.LiteJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class LiteSearchTokenSnippetTest extends LiteJerseyTest{

    @Test
    public void testSearchWithTokens () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("show-tokens", "true")
                .queryParam("context", "sentence").queryParam("count", "13")
                .request()
                .get();

        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);

        assertTrue(node.at("/matches/0/hasSnippet").asBoolean());
        assertTrue(node.at("/matches/0/hasTokens").asBoolean());
        assertTrue(node.at("/matches/0/tokens/left").size()>0);
        assertTrue(node.at("/matches/0/tokens/right").size()>0);
        assertEquals(1, node.at("/matches/0/tokens/match").size());
    }
    
    @Test
    public void testSearchWithoutTokens () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("show-tokens", "false")
                .queryParam("context", "sentence").queryParam("count", "13")
                .request()
                .get();

        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);

        assertTrue(node.at("/matches/0/hasSnippet").asBoolean());
        assertFalse(node.at("/matches/0/hasTokens").asBoolean());
        assertTrue(node.at("/matches/0/tokens").isMissingNode());
    }
    
    @Test
    public void testSearchPublicMetadataWithTokens () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("access-rewrite-disabled", "true")
                .queryParam("show-tokens", "true")
                .queryParam("context", "sentence").queryParam("count", "13")
                .request()
                .get();

        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);

        assertFalse(node.at("/matches/0/hasSnippet").asBoolean());
        assertFalse(node.at("/matches/0/hasTokens").asBoolean());
        assertTrue(node.at("/matches/0/snippet").isMissingNode());
        assertTrue(node.at("/matches/0/tokens").isMissingNode());
        
        assertEquals(StatusCodes.NOT_ALLOWED, node.at("/warnings/0/0").asInt());
    }
}
