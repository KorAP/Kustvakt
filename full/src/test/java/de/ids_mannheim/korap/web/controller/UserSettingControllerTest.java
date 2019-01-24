package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 *
 */
public class UserSettingControllerTest extends SpringJerseyTest {

    private String username = "~UserSettingTest";
    private String username2 = "~UserSettingTest2";

    private ClientResponse sendPutRequest (String username,
            Map<String, Object> map) throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path(username)
                .path("setting")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .type(MediaType.APPLICATION_JSON).entity(map)
                .put(ClientResponse.class);

        return response;
    }

    @Test
    public void testCreateSettingWithJson () throws KustvaktException {
        String json = "{\"foundry\":\"opennlp\",\"metadata\":\"author title "
                + "textSigle availability\",\"resultPerPage\":25}";

        ClientResponse response = resource().path(API_VERSION).path(username)
                .path("setting")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .type(MediaType.APPLICATION_JSON).entity(json)
                .put(ClientResponse.class);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        int numOfResult = 25;
        String metadata = "author title textSigle availability";

        testRetrieveSettings(username, "opennlp", numOfResult, metadata);

        testDeleteKey(username, numOfResult, metadata);
    }

    @Test
    public void testCreateSettingWithMap () throws KustvaktException {

        Map<String, Object> map = new HashMap<>();
        map.put("foundry", "opennlp");
        map.put("resultPerPage", 25);
        map.put("metadata", "author title textSigle availability");

        ClientResponse response = sendPutRequest(username2, map);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        testRetrieveSettings(username2, "opennlp", 25,
                "author title textSigle availability");

        testUpdateSetting(username2);
        testputRequestInvalidKey();
    }
    
    @Test
    public void testputRequestInvalidKey () throws KustvaktException {
        Map<String, Object> map = new HashMap<>();
        map.put("key/", "invalidKey");

        ClientResponse response = sendPutRequest(username2, map);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
        assertEquals("key/", node.at("/errors/0/2").asText());
    }
    
    @Test
    public void testPutDifferentUsername () throws KustvaktException {
        String json = "{\"foundry\":\"opennlp\",\"metadata\":\"author title "
                + "textSigle availability\",\"resultPerPage\":25}";

        ClientResponse response = resource().path(API_VERSION).path(username)
                .path("setting")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username2, "pass"))
                .type(MediaType.APPLICATION_JSON).entity(json)
                .put(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testGetDifferentUsername () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path(username)
                .path("setting")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username2, "pass"))
                .get(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testDeleteKeyDifferentUsername () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path(username)
                .path("setting").path("foundry")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username2, "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
    }

    private void testDeleteKey (String username, int numOfResult,
            String metadata) throws KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path(username)
                .path("setting").path("foundry")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        testRetrieveSettings(username, null, numOfResult, metadata);
    }

    private void testUpdateSetting (String username) throws KustvaktException {
        Map<String, Object> map = new HashMap<>();
        map.put("foundry", "malt");
        map.put("resultPerPage", 15);
        map.put("metadata", "author title");

        ClientResponse response = sendPutRequest(username, map);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testRetrieveSettings(username, "malt", 15, "author title");
    }
    
    private void testRetrieveSettings (String username, String foundry,
            int numOfResult, String metadata) throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path(username)
                .path("setting")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        if (foundry == null) {
            assertTrue(node.at("/foundry").isMissingNode());
        }
        else {
            assertEquals(foundry, node.at("/foundry").asText());
        }
        assertEquals(numOfResult, node.at("/resultPerPage").asInt());
        assertEquals(metadata, node.at("/metadata").asText());
    }

}
