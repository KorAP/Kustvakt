package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author hanl, margaretha
 * @lastUpdate 19/04/2017
 * EM: FIX ME: Database restructure
 */
@Disabled
@DisplayName("Resource Info Controller Test")
class ResourceInfoControllerTest extends SpringJerseyTest {

    @Test
    @DisplayName("Test Get Public Virtual Collection Info")
    void testGetPublicVirtualCollectionInfo() throws KustvaktException {
        Response response = target().path(API_VERSION).path("collection").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals(1, node.size());
    }

    @Test
    @DisplayName("Test Get Virtual Collection Info With Authentication")
    void testGetVirtualCollectionInfoWithAuthentication() throws KustvaktException {
        Response response = target().path(API_VERSION).path("collection").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015")).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(3, node.size());
    }

    @Test
    @DisplayName("Test Get Virtual Collection Info By Id")
    void testGetVirtualCollectionInfoById() throws KustvaktException {
        Response response = target().path(API_VERSION).path("collection").path("GOE-VC").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals(node.path("name").asText(), "Goethe Virtual Collection");
        assertEquals(node.path("description").asText(), "Goethe works from 1810");
    }

    @Test
    @DisplayName("Test Get Virtual Collection Info By Id Unauthorized")
    void testGetVirtualCollectionInfoByIdUnauthorized() throws KustvaktException {
        Response response = target().path(API_VERSION).path("collection").path("WPD15-VC").request().get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(), "[Cannot found public VirtualCollection with ids: [WPD15-VC]]");
    }

    @Test
    @DisplayName("Test Get Public Corpora Info")
    void testGetPublicCorporaInfo() throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(2, node.size());
    }

    @Test
    @DisplayName("Test Get Corpus Info By Id")
    void testGetCorpusInfoById() throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus").path("WPD13").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        // System.out.println(ent);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals(node.path("id").asText(), "WPD13");
    }

    @Test
    @DisplayName("Test Get Corpus Info By Id 2")
    void testGetCorpusInfoById2() throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus").path("GOE").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals(node.path("id").asText(), "GOE");
    }

    @Test
    @DisplayName("Test Get Public Foundries Info")
    void testGetPublicFoundriesInfo() throws KustvaktException {
        Response response = target().path(API_VERSION).path("foundry").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(10, node.size());
    }

    @Test
    @DisplayName("Test Get Foundry Info By Id")
    void testGetFoundryInfoById() throws KustvaktException {
        Response response = target().path(API_VERSION).path("foundry").path("tt").request().get();
        String ent = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
    }

    @Test
    @DisplayName("Test Get Unexisting Corpus Info")
    void testGetUnexistingCorpusInfo() throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus").path("ZUW19").request().get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(), "[Cannot found public Corpus with ids: [ZUW19]]");
    }

    // EM: queries for an unauthorized corpus get the same responses /
    // treatment as
    // asking for an unexisting corpus info. Does it need a specific
    // exception instead?
    @Test
    @DisplayName("Test Get Unauthorized Corpus Info")
    void testGetUnauthorizedCorpusInfo() throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus").path("BRZ10").request().get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(), "[Cannot found public Corpus with ids: [BRZ10]]");
    }
}
