package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;
/**
 * @author hanl, margaretha
 * @lastUpdate 19/04/2017
 *
 */
public class ResourceInfoTest extends FastJerseyTest {

    @Override
    public void initMethod () throws KustvaktException {
        helper().runBootInterfaces();
    }


    @BeforeClass
    public static void configure () throws Exception {
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }


    @Test
    public void testGetPublicVirtualCollections () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotNull(node);
        assertEquals(1, node.size());
    }


    @Test
    public void testGetVirtualCollectionsWithAuthentication () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(2, node.size());
    }


    @Test
    public void testGetVirtualCollectionById () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection").path("WPD15-VC").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
        assertEquals("Wikipedia Virtual Collection",
                node.path("name").asText());
        assertEquals("German Wikipedia 2015",
                node.path("description").asText());
    }


    @Test
    public void testGetPublicCorpora () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(2, node.size());
    }


    @Test
    public void testGetCorpusById () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("WPD15").get(ClientResponse.class);
        String ent = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals("WPD15", node.path("id").asText());
    }


    @Test
    public void testGetCorpusById2 () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("GOE").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals("GOE", node.path("id").asText());
    }


    @Test
    public void testGetPublicFoundries () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("foundry").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isArray());
        assertEquals(10, node.size());
    }


    @Test
    public void testGetFoundryById () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("foundry").path("tt").get(ClientResponse.class);
        String ent = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
    }


    @Test
    public void testGetUnexistingCorpus () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("ZUW19").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode error = JsonUtils.readTree(ent).get("errors").get(0);
        assertEquals(101, error.get(0).asInt());
        assertEquals("[Cannot found public resources with ids: [ZUW19]]",
                error.get(2).asText());
    }


    // EM: queries for an unauthorized corpus get the same responses / treatment as 
    // asking for an unexisting corpus. Does it need a specific exception instead?
    @Test
    public void testGetUnauthorizedCorpus () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("BRZ10").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode error = JsonUtils.readTree(ent).get("errors").get(0);
        assertEquals(101, error.get(0).asInt());
        assertEquals("[Cannot found public resources with ids: [BRZ10]]",
                error.get(2).asText());
    }


}
