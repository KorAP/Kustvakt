package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl, margaretha
 * @lastUpdate 19/04/2017
 *             EM: FIX ME: Database restructure
 */
@Disabled
public class ResourceInfoControllerTest extends SpringJerseyTest {

    @Test
    public void testGetPublicVirtualCollectionInfo () throws KustvaktException {
        Response response = target().path(API_VERSION).path("collection")
                .request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals(1, node.size());
    }

    @Test
    public void testGetVirtualCollectionInfoWithAuthentication ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("collection")
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(3, node.size());
    }

    @Test
    public void testGetVirtualCollectionInfoById () throws KustvaktException {
        Response response = target().path(API_VERSION).path("collection")
                .path("GOE-VC").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals(node.path("name").asText(), "Goethe Virtual Collection");
        assertEquals(node.path("description").asText(),
                "Goethe works from 1810");
    }

    @Test
    public void testGetVirtualCollectionInfoByIdUnauthorized ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("collection")
                .path("WPD15-VC").request().get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(),
                "[Cannot found public VirtualCollection with ids: [WPD15-VC]]");
    }

    @Test
    public void testGetPublicCorporaInfo () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus").request()
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(2, node.size());
    }

    @Test
    public void testGetCorpusInfoById () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("WPD13").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        // System.out.println(ent);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals(node.path("id").asText(), "WPD13");
    }

    @Test
    public void testGetCorpusInfoById2 () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("GOE").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals(node.path("id").asText(), "GOE");
    }

    @Test
    public void testGetPublicFoundriesInfo () throws KustvaktException {
        Response response = target().path(API_VERSION).path("foundry").request()
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(10, node.size());
    }

    @Test
    public void testGetFoundryInfoById () throws KustvaktException {
        Response response = target().path(API_VERSION).path("foundry")
                .path("tt").request().get();
        String ent = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
    }

    @Test
    public void testGetUnexistingCorpusInfo () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("ZUW19").request().get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(),
                "[Cannot found public Corpus with ids: [ZUW19]]");
    }

    // EM: queries for an unauthorized corpus get the same responses /
    // treatment as
    // asking for an unexisting corpus info. Does it need a specific
    // exception instead?
    @Test
    public void testGetUnauthorizedCorpusInfo () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("BRZ10").request().get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(),
                "[Cannot found public Corpus with ids: [BRZ10]]");
    }
}
