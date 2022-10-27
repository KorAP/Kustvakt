package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.entity.InstalledPlugin;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.InstalledPluginDao;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

public class OAuth2PluginTest extends OAuth2TestBase {

    private String username = "plugin-user";
    @Autowired
    private InstalledPluginDao pluginDao;

    @Test
    public void testRegisterPlugin () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode source = JsonUtils.readTree("{ \"plugin\" : \"source\"}");

        int refreshTokenExpiry = TimeUtils.convertTimeToSeconds("90D");

        String clientName = "Plugin";
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName(clientName);
        json.setType(OAuth2ClientType.CONFIDENTIAL);
        json.setDescription("This is a plugin test client.");
        json.setSource(source);
        json.setRefreshTokenExpiry(refreshTokenExpiry);

        ClientResponse response = registerClient(username, json);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();
        assertNotNull(clientId);
        assertNotNull(clientSecret);

        testInstallPluginNotPermitted(clientId);
        testRetrievePluginInfo(clientId, refreshTokenExpiry);

        node = listPlugins(false);
        assertEquals(3, node.size());
        node = listPlugins(true); // permitted only
        assertEquals(2, node.size());

        testListUserRegisteredPlugins(username, clientId, clientName,
                refreshTokenExpiry);
        deregisterClient(username, clientId);
    }

    @Test
    public void testRegisterPublicPlugin () throws KustvaktException {
        JsonNode source = JsonUtils.readTree("{ \"plugin\" : \"source\"}");
        String clientName = "Public Plugin";
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName(clientName);
        json.setType(OAuth2ClientType.PUBLIC);
        json.setDescription("This is a public plugin.");
        json.setSource(source);

        ClientResponse response = registerClient(username, json);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertFalse(node.at("/error_description").isMissingNode());
        
//        assertEquals(Status.OK.getStatusCode(), response.getStatus());
//        String clientId = node.at("/client_id").asText(); 
//        assertTrue(node.at("/client_secret").isMissingNode());
//        
//        deregisterClient(username, clientId);
    }

    private void testRetrievePluginInfo (String clientId,
            int refreshTokenExpiry) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode clientInfo = retrieveClientInfo(clientId, username);
        assertEquals(clientId, clientInfo.at("/client_id").asText());
        assertEquals("Plugin", clientInfo.at("/client_name").asText());

        assertEquals(OAuth2ClientType.CONFIDENTIAL.name(),
                clientInfo.at("/client_type").asText());
        assertNotNull(clientInfo.at("/client_description").asText());
        assertNotNull(clientInfo.at("/source").asText());
        assertFalse(clientInfo.at("/permitted").asBoolean());
        assertEquals(username, clientInfo.at("/registered_by").asText());
        assertNotNull(clientInfo.at("/registration_date"));
        assertEquals(refreshTokenExpiry,
                clientInfo.at("/refresh_token_expiry").asInt());
    }

    private void testListUserRegisteredPlugins (String username,
            String clientId, String clientName, int refreshTokenExpiry)
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
        assertEquals(refreshTokenExpiry,
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

        ClientResponse response = resource().path(API_VERSION).path("plugins")
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
        ClientResponse response = resource().path(API_VERSION).path("plugins")
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

        ClientResponse response = resource().path(API_VERSION).path("plugins")
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

        // assertTrue(node.at("/1/refresh_token_expiry").isMissingNode());
    }

    private JsonNode listPlugins (boolean permitted_only)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        MultivaluedMap<String, String> form = getSuperClientForm();
        if (permitted_only) {
            form.add("permitted_only", Boolean.toString(permitted_only));
        }
        ClientResponse response = resource().path(API_VERSION).path("plugins")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    private void testInstallConfidentialPlugin (String superClientId,
            String clientId, String username) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        MultivaluedMap<String, String> form = getSuperClientForm();
        form.add("client_id", clientId);
        ClientResponse response = installPlugin(form);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(clientId, node.at("/client_id").asText());
        assertEquals(superClientId, node.at("/super_client_id").asText());

        assertFalse(node.at("/name").isMissingNode());
        assertFalse(node.at("/description").isMissingNode());
        assertFalse(node.at("/url").isMissingNode());
        assertFalse(node.at("/installed_date").isMissingNode());

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testRetrieveInstalledPlugin(superClientId, clientId, username);
    }

    @Test
    public void testInstallPublicPlugin () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        MultivaluedMap<String, String> form = getSuperClientForm();
        form.add("client_id", publicClientId2);
        ClientResponse response = installPlugin(form);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(publicClientId2, node.at("/client_id").asText());
        assertEquals(superClientId, node.at("/super_client_id").asText());

        assertFalse(node.at("/name").isMissingNode());
        assertFalse(node.at("/description").isMissingNode());
        assertFalse(node.at("/url").isMissingNode());
        assertFalse(node.at("/installed_date").isMissingNode());

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testInstallPluginRedundant(form);

        testRetrieveInstalledPlugin(superClientId, publicClientId2, username);

        response = uninstallPlugin(publicClientId2, username);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = retrieveUserInstalledPlugin(getSuperClientForm());
        assertTrue(node.isEmpty());
    }

    private void testInstallPluginRedundant (
            MultivaluedMap<String, String> form)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = installPlugin(form);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PLUGIN_HAS_BEEN_INSTALLED,
                node.at("/errors/0/0").asInt());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    private void testInstallPluginNotPermitted (String clientId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = getSuperClientForm();
        form.add("client_id", clientId);
        ClientResponse response = installPlugin(form);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PLUGIN_NOT_PERMITTED,
                node.at("/errors/0/0").asInt());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInstallPluginMissingClientId ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = getSuperClientForm();
        ClientResponse response = installPlugin(form);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInstallPluginInvalidClientId ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = getSuperClientForm();
        form.add("client_id", "unknown");
        ClientResponse response = installPlugin(form);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("Unknown client: unknown",
                node.at("/error_description").asText());
        assertEquals("invalid_client", node.at("/error").asText());
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInstallPluginMissingSuperClientSecret ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("super_client_id", superClientId);

        ClientResponse response = installPlugin(form);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals("Missing parameter: super_client_secret",
                node.at("/error_description").asText());
        assertEquals("invalid_request", node.at("/error").asText());

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInstallPluginMissingSuperClientId ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        ClientResponse response = installPlugin(form);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals("Missing parameter: super_client_id",
                node.at("/error_description").asText());
        assertEquals("invalid_request", node.at("/error").asText());

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInstallPluginUnauthorizedClient ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("super_client_id", confidentialClientId);
        form.add("super_client_secret", clientSecret);

        ClientResponse response = installPlugin(form);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("unauthorized_client", node.at("/error").asText());

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    private ClientResponse installPlugin (MultivaluedMap<String, String> form)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        return resource().path(API_VERSION).path("plugins").path("install")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    private ClientResponse uninstallPlugin (String clientId, String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        MultivaluedMap<String, String> form = getSuperClientForm();
        form.add("client_id", clientId);

        return resource().path(API_VERSION).path("plugins").path("uninstall")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    private void testRetrieveInstalledPlugin (String superClientId,
            String clientId, String installedBy) throws KustvaktException {
        InstalledPlugin plugin = pluginDao
                .retrieveInstalledPlugin(superClientId, clientId, installedBy);
        assertEquals(clientId, plugin.getClient().getId());
        assertEquals(superClientId, plugin.getSuperClient().getId());
        assertEquals(installedBy, plugin.getInstalledBy());

        assertTrue(plugin.getId() > 0);
        assertTrue(plugin.getInstalledDate() != null);
    }

    @Test
    public void testListUserInstalledPlugins ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException, IOException {

        testInstallConfidentialPlugin(superClientId, confidentialClientId,
                username);
        
        JsonNode node = testRequestAccessToken(confidentialClientId);
        String accessToken = node.at("/access_token").asText(); 
        String refreshToken = node.at("/refresh_token").asText();
        testSearchWithOAuth2Token(accessToken);
        
        testInstallConfidentialPlugin(superClientId, confidentialClientId2,
                username);

        node = retrieveUserInstalledPlugin(getSuperClientForm());
        assertEquals(2, node.size());

        ClientResponse response =
                uninstallPlugin(confidentialClientId, username);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = retrieveUserInstalledPlugin(getSuperClientForm());
        assertEquals(1, node.size());

        testRequestTokenWithRevokedRefreshToken(confidentialClientId,
                clientSecret, refreshToken);
        testSearchWithRevokedAccessToken(accessToken);
        
        response = uninstallPlugin(confidentialClientId2, username);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = retrieveUserInstalledPlugin(getSuperClientForm());
        assertEquals(0, node.size());

        testUninstallNotInstalledPlugin();
    }
    
    private JsonNode testRequestAccessToken (String clientId) throws KustvaktException {
        String userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, "password");
        String code = requestAuthorizationCode(clientId, userAuthHeader);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                clientId, clientSecret, code);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        return node;
    }

    private void testUninstallNotInstalledPlugin ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response =
                uninstallPlugin(confidentialClientId2, username);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());

    }

    private JsonNode retrieveUserInstalledPlugin (
            MultivaluedMap<String, String> form)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("plugins")
                .path("installed")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        return JsonUtils.readTree(entity);

    }
}