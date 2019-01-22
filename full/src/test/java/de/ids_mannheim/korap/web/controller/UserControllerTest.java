package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

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
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 *
 */
public class UserControllerTest extends SpringJerseyTest {

    private String username = "UserControllerTest";

    private ClientResponse sendPutRequest (Map<String, Object> form)
            throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("user")
                .path("settings").path(username)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .type(MediaType.APPLICATION_JSON).entity(form)
                .put(ClientResponse.class);

        return response;
    }

    @Test
    public void testCreateSettingWithJson () throws KustvaktException {
        String json = "{\"foundry\":\"opennlp\",\"metadata\":\"author title "
                + "textSigle availability\",\"resultPerPage\":25}";

        ClientResponse response = resource().path(API_VERSION).path("user")
                .path("settings").path(username)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .type(MediaType.APPLICATION_JSON).entity(json)
                .put(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testRetrieveSettings("opennlp", 25,
                "author title textSigle availability");
    }

    @Test
    public void testCreateSettingWithForm () throws KustvaktException {
        Map<String, Object> form = new HashMap<>();
        form.put("foundry", "opennlp");
        form.put("resultPerPage", 25);
        form.put("metadata", "author title textSigle availability");

        ClientResponse response = sendPutRequest(form);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testRetrieveSettings("opennlp", 25,
                "author title textSigle availability");

        testUpdateSetting();
    }

    private void testUpdateSetting () throws KustvaktException {
        Map<String, Object> form = new HashMap<>();
        form.put("foundry", "malt");
        form.put("resultPerPage", 15);
        form.put("metadata", "author title");

        ClientResponse response = sendPutRequest(form);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testRetrieveSettings("malt", 15, "author title");

    }

    private void testRetrieveSettings (String foundry, int numOfResult,
            String metadata) throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("user")
                .path("settings")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(foundry, node.at("/foundry").asText());
        assertEquals(numOfResult, node.at("/resultPerPage").asInt());
        assertEquals(metadata, node.at("/metadata").asText());
    }

}
