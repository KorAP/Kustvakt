package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.client.Entity;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 */
public class UserSettingControllerTest extends SpringJerseyTest {

    private String username = "UserSetting_Test";

    private String username2 = "UserSetting.Test2";

    public Response sendPutRequest(String username, Map<String, Object> map) throws KustvaktException {
        Response response = target().path(API_VERSION).path("~" + username).path("setting").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).put(Entity.json(map));
        return response;
    }

    @Test
    public void testCreateSettingWithJson() throws KustvaktException {
        String json = "{\"pos-foundry\":\"opennlp\",\"metadata\":[\"author\", \"title\"," + "\"textSigle\", \"availability\"],\"resultPerPage\":25}";
        Response response = target().path(API_VERSION).path("~" + username).path("setting").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).put(Entity.json(json));
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        int numOfResult = 25;
        String metadata = "[\"author\",\"title\",\"textSigle\"," + "\"availability\"]";
        testRetrieveSettings(username, "opennlp", numOfResult, metadata, true);
        testDeleteKeyNotExist(username);
        testDeleteKey(username, numOfResult, metadata, true);
        testDeleteSetting(username);
    }

    @Test
    public void testCreateSettingWithMap() throws KustvaktException {
        Map<String, Object> map = new HashMap<>();
        map.put("pos-foundry", "opennlp");
        map.put("resultPerPage", 25);
        map.put("metadata", "author title textSigle availability");
        Response response = sendPutRequest(username2, map);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        testRetrieveSettings(username2, "opennlp", 25, "author title textSigle availability", false);
        testUpdateSetting(username2);
        testputRequestInvalidKey();
    }

    @Test
    public void testputRequestInvalidKey() throws KustvaktException {
        Map<String, Object> map = new HashMap<>();
        map.put("key/", "invalidKey");
        Response response = sendPutRequest(username2, map);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(), "key/");
    }

    @Test
    public void testPutDifferentUsername() throws KustvaktException {
        String json = "{\"pos-foundry\":\"opennlp\",\"metadata\":\"author title " + "textSigle availability\",\"resultPerPage\":25}";
        Response response = target().path(API_VERSION).path("~" + username).path("setting").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username2, "pass")).put(Entity.json(json));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
    }

    @Test
    public void testGetDifferentUsername() throws KustvaktException {
        Response response = target().path(API_VERSION).path("~" + username).path("setting").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username2, "pass")).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
    }

    @Test
    public void testGetSettingNotExist() throws KustvaktException {
        String username = "tralala";
        Response response = target().path(API_VERSION).path("~" + username).path("setting").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND, node.at("/errors/0/0").asInt());
        assertEquals("No default setting for username: " + username + " is found", node.at("/errors/0/1").asText());
        assertEquals(username, node.at("/errors/0/2").asText());
    }

    @Test
    public void testDeleteSettingNotExist() throws KustvaktException {
        String username = "tralala";
        Response response = target().path(API_VERSION).path("~" + username).path("setting").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteKeyDifferentUsername() throws KustvaktException {
        Response response = target().path(API_VERSION).path("~" + username).path("setting").path("pos-foundry").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username2, "pass")).delete();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
    }

    private void testDeleteSetting(String username) throws KustvaktException {
        Response response = target().path(API_VERSION).path("~" + username).path("setting").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response = target().path(API_VERSION).path("~" + username).path("setting").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND, node.at("/errors/0/0").asInt());
        assertEquals(username, node.at("/errors/0/2").asText());
    }

    // EM: deleting a non-existing key does not throw an error,
    // because
    // the purpose of the request has been achieved.
    private void testDeleteKeyNotExist(String username) throws KustvaktException {
        Response response = target().path(API_VERSION).path("~" + username).path("setting").path("lemma-foundry").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testDeleteKey(String username, int numOfResult, String metadata, boolean isMetadataArray) throws KustvaktException {
        Response response = target().path(API_VERSION).path("~" + username).path("setting").path("pos-foundry").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        testRetrieveSettings(username, null, numOfResult, metadata, isMetadataArray);
    }

    private void testUpdateSetting(String username) throws KustvaktException {
        Map<String, Object> map = new HashMap<>();
        map.put("pos-foundry", "malt");
        map.put("resultPerPage", 15);
        map.put("metadata", "author title");
        Response response = sendPutRequest(username, map);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        testRetrieveSettings(username, "malt", 15, "author title", false);
    }

    private void testRetrieveSettings(String username, String posFoundry, int numOfResult, String metadata, boolean isMetadataArray) throws KustvaktException {
        Response response = target().path(API_VERSION).path("~" + username).path("setting").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        if (posFoundry == null) {
            assertTrue(node.at("/pos-foundry").isMissingNode());
        } else {
            assertEquals(posFoundry, node.at("/pos-foundry").asText());
        }
        assertEquals(numOfResult, node.at("/resultPerPage").asInt());
        if (isMetadataArray) {
            assertEquals(metadata, node.at("/metadata").toString());
        } else {
            assertEquals(metadata, node.at("/metadata").asText());
        }
    }
}
