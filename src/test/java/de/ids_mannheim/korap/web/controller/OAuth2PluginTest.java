package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
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
    public void testRegisterPlugin ()
            throws ProcessingException, KustvaktException {
        JsonNode source = JsonUtils.readTree("{ \"plugin\" : \"source\"}");
        int refreshTokenExpiry = TimeUtils.convertTimeToSeconds("90D");
        String clientName = "Plugin";
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName(clientName);
        json.setType(OAuth2ClientType.CONFIDENTIAL);
        json.setDescription("This is a plugin test client.");
        json.setSource(source);
        json.setRefreshTokenExpiry(refreshTokenExpiry);
        Response response = registerClient(username, json);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();
        assertNotNull(clientId);
        assertNotNull(clientSecret);
        testInstallPluginNotPermitted(clientId);
        testRetrievePluginInfo(clientId, refreshTokenExpiry);
        node = listPlugins(false);
        assertEquals(3, node.size());
        // permitted only
        node = listPlugins(true);
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
        Response response = registerClient(username, json);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertFalse(node.at("/error_description").isMissingNode());
        // assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // String clientId = node.at("/client_id").asText();
        // assertTrue(node.at("/client_secret").isMissingNode());
        // 
        // deregisterClient(username, clientId);
    }

    private void testRetrievePluginInfo (String clientId,
            int refreshTokenExpiry)
            throws ProcessingException, KustvaktException {
        JsonNode clientInfo = retrieveClientInfo(clientId, username);
        assertEquals(clientId, clientInfo.at("/client_id").asText());
        assertEquals(clientInfo.at("/client_name").asText(), "Plugin");
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
            throws ProcessingException, KustvaktException {
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
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("super_client_id", publicClientId);
        testListPluginsClientUnauthorized(form);
    }

    @Test
    public void testListPluginsUnauthorizedConfidential ()
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("super_client_id", confidentialClientId2);
        form.param("super_client_secret", clientSecret);
        testListPluginsClientUnauthorized(form);
    }

    @Test
    public void testListPluginsMissingClientSecret ()
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("super_client_id", confidentialClientId);
        Response response = target().path(API_VERSION).path("plugins").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertFalse(node.at("/error_description").isMissingNode());
    }

    private void testListPluginsClientUnauthorized (Form form)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("plugins").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(OAuth2Error.UNAUTHORIZED_CLIENT,
                node.at("/error").asText());
        assertFalse(node.at("/error_description").isMissingNode());
    }

    @Test
    public void testListPluginsUserUnauthorized ()
            throws ProcessingException, KustvaktException {
        Form form = getSuperClientForm();
        Response response = target().path(API_VERSION).path("plugins").request()
                .header(Attributes.AUTHORIZATION, "Bearer blahblah")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testListPluginsConcurrent () throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(executorService
                    .submit(new PluginListCallable("Thread " + (i + 1))));
        }
        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);
        for (Future<Void> future : futures) {
            try {
                // This will re-throw any exceptions
                future.get();
                // that occurred in threads
            }
            catch (ExecutionException e) {
                fail("Test failed: " + e.getCause().getMessage());
            }
        }
    }

    class PluginListCallable implements Callable<Void> {

        private final String name;

        public PluginListCallable (String name) {
            this.name = name;
        }

        @Override
        public Void call () {
            Form form = getSuperClientForm();
            try {
                Response response = target().path(API_VERSION).path("plugins")
                        .request()
                        .header(Attributes.AUTHORIZATION,
                                HttpAuthorizationHandler
                                        .createBasicAuthorizationHeaderValue(
                                                username, "pass"))
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .post(Entity.form(form));
                assertEquals(Status.OK.getStatusCode(), response.getStatus());
                String entity = response.readEntity(String.class);
                JsonNode node = JsonUtils.readTree(entity);
                assertEquals(2, node.size());
            }
            catch (KustvaktException e) {
                e.printStackTrace();
                throw new RuntimeException(name, e);
            }
            return null;
        }
    }

    @Test
    public void testListAllPlugins ()
            throws ProcessingException, KustvaktException {
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
            throws ProcessingException, KustvaktException {
        Form form = getSuperClientForm();
        if (permitted_only) {
            form.param("permitted_only", Boolean.toString(permitted_only));
        }
        Response response = target().path(API_VERSION).path("plugins").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    private void testInstallConfidentialPlugin (String superClientId,
            String clientId, String username)
            throws ProcessingException, KustvaktException {
        Form form = getSuperClientForm();
        form.param("client_id", clientId);
        Response response = installPlugin(form);
        String entity = response.readEntity(String.class);
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
    public void testInstallPublicPlugin ()
            throws ProcessingException, KustvaktException {
        Form form = getSuperClientForm();
        form.param("client_id", publicClientId2);
        Response response = installPlugin(form);
        String entity = response.readEntity(String.class);
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

    private void testInstallPluginRedundant (Form form)
            throws ProcessingException, KustvaktException {
        Response response = installPlugin(form);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PLUGIN_HAS_BEEN_INSTALLED,
                node.at("/errors/0/0").asInt());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    private void testInstallPluginNotPermitted (String clientId)
            throws ProcessingException, KustvaktException {
        Form form = getSuperClientForm();
        form.param("client_id", clientId);
        Response response = installPlugin(form);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PLUGIN_NOT_PERMITTED,
                node.at("/errors/0/0").asInt());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInstallPluginMissingClientId ()
            throws ProcessingException, KustvaktException {
        Form form = getSuperClientForm();
        Response response = installPlugin(form);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInstallPluginInvalidClientId ()
            throws ProcessingException, KustvaktException {
        Form form = getSuperClientForm();
        form.param("client_id", "unknown");
        Response response = installPlugin(form);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/error_description").asText(),
                "Unknown client: unknown");
        assertEquals(node.at("/error").asText(), "invalid_client");
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInstallPluginMissingSuperClientSecret ()
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("super_client_id", superClientId);
        Response response = installPlugin(form);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/error_description").asText(),
                "Missing parameter: super_client_secret");
        assertEquals(node.at("/error").asText(), "invalid_request");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInstallPluginMissingSuperClientId ()
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        Response response = installPlugin(form);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/error_description").asText(),
                "Missing parameter: super_client_id");
        assertEquals(node.at("/error").asText(), "invalid_request");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testInstallPluginUnauthorizedClient ()
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("super_client_id", confidentialClientId);
        form.param("super_client_secret", clientSecret);
        Response response = installPlugin(form);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/error").asText(), "unauthorized_client");
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    private Response installPlugin (Form form)
            throws ProcessingException, KustvaktException {
        return target().path(API_VERSION).path("plugins").path("install")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
    }

    private Response uninstallPlugin (String clientId, String username)
            throws ProcessingException, KustvaktException {
        Form form = getSuperClientForm();
        form.param("client_id", clientId);
        return target().path(API_VERSION).path("plugins").path("uninstall")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
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
            throws ProcessingException, KustvaktException, IOException {
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
        Response response = uninstallPlugin(confidentialClientId, username);
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
        testReinstallUninstalledPlugin();
        testUninstallNotInstalledPlugin();
    }

    private void testReinstallUninstalledPlugin ()
            throws ProcessingException, KustvaktException {
        testInstallConfidentialPlugin(superClientId, confidentialClientId2,
                username);
        JsonNode node = retrieveUserInstalledPlugin(getSuperClientForm());
        assertEquals(1, node.size());
        Response response = uninstallPlugin(confidentialClientId2, username);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = retrieveUserInstalledPlugin(getSuperClientForm());
        assertEquals(0, node.size());
    }

    private JsonNode testRequestAccessToken (String clientId)
            throws KustvaktException {
        String userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, "password");
        String code = requestAuthorizationCode(clientId, userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(clientId,
                clientSecret, code);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        return node;
    }

    private void testUninstallNotInstalledPlugin ()
            throws ProcessingException, KustvaktException {
        Response response = uninstallPlugin(confidentialClientId2, username);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
    }

    private JsonNode retrieveUserInstalledPlugin (Form form)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("plugins")
                .path("installed").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }
}
