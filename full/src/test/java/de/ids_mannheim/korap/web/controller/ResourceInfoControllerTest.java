package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response.Status;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.FastJerseyTest;

/**
 * @author hanl, margaretha
 * @lastUpdate 19/04/2017
 *             EM: FIX ME: Database restructure
 */
@Ignore
public class ResourceInfoControllerTest extends FastJerseyTest {

    @Test
    public void testGetPublicVirtualCollectionInfo () throws KustvaktException {
        Response response = target().path(getAPIVersion())
                .path("collection")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals(1, node.size());
    }


    @Test
    public void testGetVirtualCollectionInfoWithAuthentication ()
            throws KustvaktException {
        Response response = target().path(getAPIVersion())
                .path("collection")
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(3, node.size());
    }


    @Test
    public void testGetVirtualCollectionInfoById () throws KustvaktException {
        Response response = target().path(getAPIVersion())
                .path("collection").path("GOE-VC")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals("Goethe Virtual Collection", node.path("name").asText());
        assertEquals("Goethe works from 1810",
                node.path("description").asText());
    }

    @Test
    public void testGetVirtualCollectionInfoByIdUnauthorized ()
            throws KustvaktException {
        Response response = target().path(getAPIVersion())
                .path("collection").path("WPD15-VC")
                .request()
                .get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals(
                "[Cannot found public VirtualCollection with ids: [WPD15-VC]]",
                node.at("/errors/0/2").asText());
    }

    @Test
    public void testGetPublicCorporaInfo () throws KustvaktException {
        Response response = target().path(getAPIVersion())
                .path("corpus")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(2, node.size());
    }


    @Test
    public void testGetCorpusInfoById () throws KustvaktException {
        Response response = target().path(getAPIVersion())
                .path("corpus").path("WPD13")
                .request()
                .get();

        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        // System.out.println(ent);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals("WPD13", node.path("id").asText());
    }


    @Test
    public void testGetCorpusInfoById2 () throws KustvaktException {
        Response response = target().path(getAPIVersion())
                .path("corpus").path("GOE")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals("GOE", node.path("id").asText());
    }


    @Test
    public void testGetPublicFoundriesInfo () throws KustvaktException {
        Response response = target().path(getAPIVersion())
                .path("foundry")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(10, node.size());
    }


    @Test
    public void testGetFoundryInfoById () throws KustvaktException {
        Response response = target().path(getAPIVersion())
                .path("foundry").path("tt")
                .request()
                .get();
        String ent = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
    }


    @Test
    public void testGetUnexistingCorpusInfo () throws KustvaktException {
        Response response = target().path(getAPIVersion())
                .path("corpus").path("ZUW19")
                .request()
                .get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals("[Cannot found public Corpus with ids: [ZUW19]]",
                node.at("/errors/0/2").asText());
    }


    // EM: queries for an unauthorized corpus get the same responses /
    // treatment as
    // asking for an unexisting corpus info. Does it need a specific
    // exception instead?
    @Test
    public void testGetUnauthorizedCorpusInfo () throws KustvaktException {
        Response response = target().path(getAPIVersion())
                .path("corpus").path("BRZ10")
                .request()
                .get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals("[Cannot found public Corpus with ids: [BRZ10]]",
                node.at("/errors/0/2").asText());
    }


}
