package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

public class OAuth2PluginTest extends OAuth2TestBase {

    private String username = "plugin-user";

    @Test
    public void testRegisterPlugin () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode source = JsonUtils.readTree("{ \"plugin\" : \"source\"}");

        String clientName = "Plugin";
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName(clientName);
        json.setType(OAuth2ClientType.CONFIDENTIAL);
        json.setDescription("This is a plugin test client.");
        json.setSource(source);

        ClientResponse response = registerClient(username, json);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();
        assertNotNull(clientId);
        assertNotNull(clientSecret);

        testRetrievePluginInfo(clientId);

        node = listPlugins(false);
        assertEquals(3, node.size());
        node = listPlugins(true); // permitted only
        assertEquals(2, node.size());

        testListUserRegisteredPlugins(username, clientId, clientName);
        deregisterConfidentialClient(username, clientId);
    }

    private void testRetrievePluginInfo (String clientId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        JsonNode clientInfo = retrieveClientInfo(clientId, username);
        assertEquals(clientId, clientInfo.at("/id").asText());
        assertEquals("Plugin", clientInfo.at("/name").asText());

        assertEquals(OAuth2ClientType.CONFIDENTIAL.name(),
                clientInfo.at("/type").asText());
        assertNotNull(clientInfo.at("/description").asText());
        assertNotNull(clientInfo.at("/source").asText());
        assertFalse(clientInfo.at("/permitted").asBoolean());
        assertEquals(username, clientInfo.at("/registered_by").asText());
        assertNotNull(clientInfo.at("/registration_date"));
        assertEquals(defaultRefreshTokenExpiry,
                clientInfo.at("/refresh_token_expiry").asInt());
    }

    private void testListUserRegisteredPlugins (String username,
            String clientId, String clientName)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        JsonNode node = listUserRegisteredClients(username);
        assertEquals(1, node.size());
        assertEquals(clientId, node.at("/0/client_id").asText());
        assertEquals(clientName, node.at("/0/client_name").asText());
        assertEquals(OAuth2ClientType.CONFIDENTIAL.name(),
                node.at("/0/client_type").asText());
        assertFalse(node.at("/0/permitted").asBoolean());
        assertFalse(node.at("/0/registration_date").isMissingNode());
        assertFalse(node.at("/0/source").isMissingNode());
        assertEquals(defaultRefreshTokenExpiry,
                node.at("/0/refresh_token_expiry").asInt());
    }

    @Test
    public void testListPluginsUnauthorizedPublic ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("super_client_id", publicClientId);
        testListPluginsClientUnauthorized(form);
    }

    @Test
    public void testListPluginsUnauthorizedConfidential ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("super_client_id", confidentialClientId2);
        form.add("super_client_secret", clientSecret);
        testListPluginsClientUnauthorized(form);
    }

    @Test
    public void testListPluginsMissingClientSecret ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("super_client_id", confidentialClientId);

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("plugins")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertFalse(node.at("/error_description").isMissingNode());
    }

    private void testListPluginsClientUnauthorized (
            MultivaluedMap<String, String> form)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("plugins")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        assertEquals(OAuth2Error.UNAUTHORIZED_CLIENT,
                node.at("/error").asText());
        assertFalse(node.at("/error_description").isMissingNode());
    }

    @Test
    public void testListPluginsUserUnauthorized ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("plugins")
                .header(Attributes.AUTHORIZATION, "Bearer blahblah")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(getSuperClientForm()).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testListAllPlugins () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode node = listPlugins(false);
        assertEquals(2, node.size());
        assertFalse(node.at("/0/client_id").isMissingNode());
        assertFalse(node.at("/0/client_name").isMissingNode());
        assertFalse(node.at("/0/client_description").isMissingNode());
        assertFalse(node.at("/0/client_type").isMissingNode());
        assertFalse(node.at("/0/permitted").isMissingNode());
        assertFalse(node.at("/0/registration_date").isMissingNode());
        assertFalse(node.at("/0/source").isMissingNode());
        assertFalse(node.at("/0/refresh_token_expiry").isMissingNode());
        
        assertTrue(node.at("/1/refresh_token_expiry").isMissingNode());
    }

    private JsonNode listPlugins (boolean permitted_only)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        MultivaluedMap<String, String> form = getSuperClientForm();
        if (permitted_only) {
            form.add("permitted_only", Boolean.toString(permitted_only));
        }
        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("plugins")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        return JsonUtils.readTree(entity);
    }

}
