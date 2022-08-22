package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class SearchTokenSnippetTest extends SpringJerseyTest {

    @Test
    public void testSearchWithTokens () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("show-tokens", "true")
                .queryParam("context", "sentence").queryParam("count", "13")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);

        assertTrue(node.at("/matches/0/hasSnippet").asBoolean());
        assertTrue(node.at("/matches/0/hasTokens").asBoolean());
        assertTrue(node.at("/matches/0/tokens/left").size()>0);
        assertTrue(node.at("/matches/0/tokens/right").size()>0);
        assertEquals(1, node.at("/matches/0/tokens/match").size());
    }
    
    @Test
    public void testSearchWithoutTokens () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("show-tokens", "false")
                .queryParam("context", "sentence").queryParam("count", "13")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);

        assertTrue(node.at("/matches/0/hasSnippet").asBoolean());
        assertFalse(node.at("/matches/0/hasTokens").asBoolean());
        assertTrue(node.at("/matches/0/tokens").isMissingNode());
    }
    
    @Test
    public void testSearchPublicMetadataWithTokens () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("access-rewrite-disabled", "true")
                .queryParam("show-tokens", "true")
                .queryParam("context", "sentence").queryParam("count", "13")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);

        assertFalse(node.at("/matches/0/hasSnippet").asBoolean());
        assertFalse(node.at("/matches/0/hasTokens").asBoolean());
        assertTrue(node.at("/matches/0/snippet").isMissingNode());
        assertTrue(node.at("/matches/0/tokens").isMissingNode());
        
        assertEquals(StatusCodes.NOT_ALLOWED, node.at("/warnings/0/0").asInt());
    }
}