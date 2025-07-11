package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @author margaretha
 */
public class OAuth2ClientControllerTest extends OAuth2TestBase {

    private String username = "OAuth2ClientControllerTest";

    private String userAuthHeader;

    public OAuth2ClientControllerTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
    }

    @Test
    public void testRetrieveClientInfo () throws KustvaktException {
        // public client plugin
        JsonNode clientInfo = retrieveClientInfo(publicClientId, "system");
        assertEquals(publicClientId, clientInfo.at("/client_id").asText());
        assertEquals(clientInfo.at("/client_name").asText(),
                "public client plugin with redirect uri");
        assertNotNull(clientInfo.at("/client_description"));
        assertNotNull(clientInfo.at("/client_url"));
        assertEquals("PUBLIC", clientInfo.at("/client_type").asText());
//        assertEquals("system", clientInfo.at("/registered_by").asText());
        // confidential client
        clientInfo = retrieveClientInfo(confidentialClientId, "system");
        assertEquals(confidentialClientId,
                clientInfo.at("/client_id").asText());
        assertEquals(clientInfo.at("/client_name").asText(),
                "non super confidential client");
        assertNotNull(clientInfo.at("/client_url"));
        assertNotNull(clientInfo.at("/redirect_uri"));
        assertEquals(false, clientInfo.at("/super").asBoolean());
        assertEquals("CONFIDENTIAL", clientInfo.at("/client_type").asText());
        // super client
        clientInfo = retrieveClientInfo(superClientId, "system");
        assertEquals(superClientId, clientInfo.at("/client_id").asText());
        assertEquals("super confidential client",
                clientInfo.at("/client_name").asText());
        assertNotNull(clientInfo.at("/client_url"));
        assertNotNull(clientInfo.at("/redirect_uri"));
        assertEquals("CONFIDENTIAL", clientInfo.at("/client_type").asText());
        assertTrue(clientInfo.at("/super").asBoolean());
    }

    @Test
    public void testRegisterConfidentialClient () throws KustvaktException {
        Response response = registerConfidentialClient(username);
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();
        assertNotNull(clientId);
        assertNotNull(clientSecret);
        assertFalse(clientId.contains("a"));
        testListConfidentialClient(username, clientId);
        testConfidentialClientInfo(clientId, username);
//        testListAllUserClients(username);
        testResetConfidentialClientSecret(clientId, clientSecret);
        deregisterClient(username, clientId);
    }

    @Test
    public void testRegisterClientNameTooShort ()
            throws ProcessingException, KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2ClientJson("R",
                OAuth2ClientType.PUBLIC, null);
        Response response = registerClient(username, clientJson);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/error_description").asText(),
                "client_name must contain at least 3 characters");
        assertEquals(node.at("/error").asText(), "invalid_request");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientEmptyName ()
            throws ProcessingException, KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2ClientJson("",
                OAuth2ClientType.PUBLIC, null);
        Response response = registerClient(username, clientJson);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/error_description").asText(),
                "client_name must contain at least 3 characters");
        assertEquals(node.at("/error").asText(), "invalid_request");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientMissingName ()
            throws ProcessingException, KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2ClientJson(null,
                OAuth2ClientType.PUBLIC, null);
        Response response = registerClient(username, clientJson);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/error_description").asText(),
                "client_name is null");
        assertEquals(node.at("/error").asText(), "invalid_request");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientMissingDescription ()
            throws ProcessingException, KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2ClientJson("R client",
                OAuth2ClientType.PUBLIC, null);
        Response response = registerClient(username, clientJson);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/error_description").asText(),
                "client_description is null");
        assertEquals(node.at("/error").asText(), "invalid_request");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientMissingType ()
            throws ProcessingException, KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2ClientJson("R client", null,
                null);
        Response response = registerClient(username, clientJson);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/error_description").asText(),
                "client_type is null");
        assertEquals(node.at("/error").asText(), "invalid_request");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientInvalidRedirectURI ()
            throws ProcessingException, KustvaktException {
        // invalid hostname
        String redirectUri = "https://test.public.client/redirect";
        OAuth2ClientJson clientJson = createOAuth2ClientJson(
                "OAuth2PublicClient", OAuth2ClientType.PUBLIC,
                "A public test client.");
        clientJson.setRedirectURI(redirectUri);
        Response response = registerClient(username, clientJson);
        testInvalidRedirectUri(response.readEntity(String.class),
                response.getHeaderString("Content-Type"), false,
                response.getStatus());
        // localhost is not allowed
        // redirectUri = "http://localhost:1410";
        // clientJson.setRedirectURI(redirectUri);
        // response = registerClient(username, clientJson);
        // testInvalidRedirectUri(response.readEntity(String.class), false,
        // response.getStatus());
        // fragment is not allowed
        redirectUri = "https://public.client.com/redirect.html#bar";
        clientJson.setRedirectURI(redirectUri);
        response = registerClient(username, clientJson);
        testInvalidRedirectUri(response.readEntity(String.class),
                response.getHeaderString("Content-Type"), false,
                response.getStatus());
    }

    @Test
    public void testRegisterPublicClientWithRefreshTokenExpiry ()
            throws ProcessingException, KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2ClientJson(
                "OAuth2PublicClient", OAuth2ClientType.PUBLIC,
                "A public test client.");
        clientJson.setRefreshTokenExpiry(31535000);
        Response response = registerClient(username, clientJson);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(node.at("/error").asText(), "invalid_request");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterConfidentialClientWithRefreshTokenExpiry ()
            throws ProcessingException, KustvaktException {
        int expiry = 31535000;
        OAuth2ClientJson clientJson = createOAuth2ClientJson(
                "OAuth2 Confidential Client", OAuth2ClientType.CONFIDENTIAL,
                "A confidential client.");
        clientJson.setRefreshTokenExpiry(expiry);
        Response response = registerClient(username, clientJson);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String clientId = node.at("/client_id").asText();
        JsonNode clientInfo = retrieveClientInfo(clientId, username);
        assertEquals(expiry, clientInfo.at("/refresh_token_expiry").asInt());
        deregisterClient(username, clientId);
    }

    @Test
    public void testRegisterConfidentialClientWithInvalidRefreshTokenExpiry ()
            throws ProcessingException, KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2ClientJson(
                "OAuth2 Confidential Client", OAuth2ClientType.CONFIDENTIAL,
                "A confidential client.");
        clientJson.setRefreshTokenExpiry(31537000);
        Response response = registerClient(username, clientJson);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(node.at("/error_description").asText(),
                "Maximum refresh token expiry is 31536000 seconds (1 year)");
        assertEquals(node.at("/error").asText(), "invalid_request");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientInvalidURL ()
            throws ProcessingException, KustvaktException {
        // invalid hostname
        String url = "https://test.public.client";
        OAuth2ClientJson clientJson = createOAuth2ClientJson(
                "OAuth2PublicClient", OAuth2ClientType.PUBLIC,
                "A public test client.");
        clientJson.setUrl(url);
        Response response = registerClient(username, clientJson);
        testInvalidUrl(response.readEntity(String.class), response.getStatus());
        // localhost is not allowed
        url = "http://localhost:1410";
        clientJson.setRedirectURI(url);
        response = registerClient(username, clientJson);
        testInvalidUrl(response.readEntity(String.class), response.getStatus());
    }

    private void testInvalidUrl (String entity, int status)
            throws KustvaktException {
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals(node.at("/error_description").asText(), "Invalid URL");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), status);
    }

    @Test
    public void testRegisterPublicClient ()
            throws ProcessingException, KustvaktException {
        String redirectUri = "https://public.client.com/redirect";
        OAuth2ClientJson clientJson = createOAuth2ClientJson(
                "OAuth2PublicClient", OAuth2ClientType.PUBLIC,
                "A public test client.");
        // http and fragment are allowed
        clientJson.setUrl("http://public.client.com/index.html#bar");
        clientJson.setRedirectURI(redirectUri);
        Response response = registerClient(username, clientJson);
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());
        
        node = listUserClients(username,"owned_only");
        assertFalse(node.at("/0/client_redirect_uri").isMissingNode());
        assertFalse(node.at("/0/registration_date").isMissingNode());
//        assertEquals(username,
//                node.at("/0/registered_by").asText());

        testRegisterClientUnauthorizedScope(clientId);
        testResetPublicClientSecret(clientId);
        testAccessTokenAfterDeregistration(clientId, null, "");
    }

    private void testRegisterClientUnauthorizedScope (String clientId)
            throws ProcessingException, KustvaktException {
        String userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
        String code = requestAuthorizationCode(clientId, userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(clientId,
                clientSecret, code);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(node.at("/scope").asText(), "match_info search");
        String accessToken = node.at("/access_token").asText();
        OAuth2ClientJson clientJson = createOAuth2ClientJson("R client",
                OAuth2ClientType.PUBLIC, null);
        response = target().path(API_VERSION).path("oauth2").path("client")
                .path("register").request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(Entity.json(clientJson));
        String entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Scope register_client is not authorized");
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientUsingPlainJson ()
            throws ProcessingException, KustvaktException, IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("json/oauth2_public_client.json");
        String json = IOUtils.toString(is, Charset.defaultCharset());
        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path("register").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(username,
                                        "password"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(Entity.json(json));
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());
        testResetPublicClientSecret(clientId);
        testAccessTokenAfterDeregistration(clientId, null, "");
    }

    @Test
    public void testRegisterDesktopApp ()
            throws ProcessingException, KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2ClientJson(
                "OAuth2DesktopClient", OAuth2ClientType.PUBLIC,
                "This is a desktop test client.");
        Response response = registerClient(username, clientJson);
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());
        testDeregisterPublicClientMissingUserAuthentication(clientId);
        testDeregisterPublicClientMissingId();
        testDeregisterPublicClient(clientId, username);
    }

    @Test
    public void testRegisterMultipleDesktopApps ()
            throws ProcessingException, KustvaktException {
        // First client
        OAuth2ClientJson clientJson = createOAuth2ClientJson(
                "OAuth2DesktopClient1", OAuth2ClientType.PUBLIC,
                "A desktop test client.");
        Response response = registerClient(username, clientJson);
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId1 = node.at("/client_id").asText();
        assertNotNull(clientId1);
        assertTrue(node.at("/client_secret").isMissingNode());
        // Second client
        clientJson = createOAuth2ClientJson("OAuth2DesktopClient2",
                OAuth2ClientType.PUBLIC, "Another desktop test client.");
        response = registerClient(username, clientJson);
        entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(entity);
        String clientId2 = node.at("/client_id").asText();
        assertNotNull(clientId2);
        assertTrue(node.at("/client_secret").isMissingNode());
        testResetPublicClientSecret(clientId1);
        testAccessTokenAfterDeregistration(clientId1, null,
                "https://OAuth2DesktopClient1.com");
        testResetPublicClientSecret(clientId2);
        testAccessTokenAfterDeregistration(clientId2, null,
                "https://OAuth2DesktopClient2.com");
    }

    private void testAccessTokenAfterDeregistration (String clientId,
            String clientSecret, String redirectUri) throws KustvaktException {
        String userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
        String code = requestAuthorizationCode(clientId, redirectUri,
                userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(clientId,
                clientSecret, code, redirectUri);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken = node.at("/access_token").asText();
        response = searchWithAccessToken(accessToken);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        code = requestAuthorizationCode(clientId, redirectUri, userAuthHeader);
        testDeregisterPublicClient(clientId, username);
        response = requestTokenWithAuthorizationCodeAndForm(clientId,
                clientSecret, code, redirectUri);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(OAuth2Error.INVALID_CLIENT.toString(),
                node.at("/error").asText());
        response = searchWithAccessToken(accessToken);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Access token is invalid");
    }

    private void testDeregisterPublicClientMissingUserAuthentication (
            String clientId) throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path("deregister").path(clientId).request()
                .delete();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    private void testDeregisterPublicClientMissingId ()
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path("deregister").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();
        assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(),
                response.getStatus());
    }

    private void testDeregisterPublicClient (String clientId, String username)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path("deregister").path(clientId).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testResetPublicClientSecret (String clientId)
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("client_id", clientId);
        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path("reset").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals(node.at("/error_description").asText(),
                "Operation is not allowed for public clients");
    }

    private String testResetConfidentialClientSecret (String clientId,
            String clientSecret) throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("client_id", clientId);
        form.param("client_secret", clientSecret);
        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path("reset").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(clientId, node.at("/client_id").asText());
        String newClientSecret = node.at("/client_secret").asText();
        assertTrue(!clientSecret.equals(newClientSecret));
        return newClientSecret;
    }

    private void testListAuthorizedClients (String userAuthHeader)
            throws KustvaktException {
        Form form = getSuperClientForm();
        // deprecated, use filter_by = authorized_only instead
//        form.param("authorized_only", "true");
        
        form.param("filter_by", "authorized_only");
        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path("list").request()
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals(2, node.size());
        assertEquals(confidentialClientId, node.at("/0/client_id").asText());
        assertEquals(publicClientId, node.at("/1/client_id").asText());
        assertEquals(node.at("/0/client_name").asText(),
                "non super confidential client");
        assertEquals(node.at("/0/client_type").asText(), "CONFIDENTIAL");
        assertFalse(node.at("/0/client_url").isMissingNode());
        assertFalse(node.at("/0/client_description").isMissingNode());
    }

    @Test
    public void testListPublicClient () throws KustvaktException {
        String clientName = "OAuth2DoryClient";
        OAuth2ClientJson json = createOAuth2ClientJson(clientName,
                OAuth2ClientType.PUBLIC, "Dory's client.");
        registerClient("dory", json);
        JsonNode node = listUserClients("dory","owned_only");
        assertEquals(1, node.size());
        assertEquals(clientName, node.at("/0/client_name").asText());
        assertEquals(OAuth2ClientType.PUBLIC.name(),
                node.at("/0/client_type").asText());
        assertTrue(node.at("/0/permitted").asBoolean());
        assertFalse(node.at("/0/registration_date").isMissingNode());
        assertTrue(node.at("/refresh_token_expiry").isMissingNode());
        String clientId = node.at("/0/client_id").asText();
        
//        testListAllUserClients("dory");
        testDeregisterPublicClient(clientId, "dory");
    }

    private void testListConfidentialClient (String username, String clientId)
            throws ProcessingException, KustvaktException {
        // means authorized_only = false
        // this is deprecated, filter_by = owned_only should be use instead.
        JsonNode node = listUserClients(username,"");
        assertEquals(1, node.size());
        assertEquals(clientId, node.at("/0/client_id").asText());
        assertEquals(node.at("/0/client_name").asText(), "OAuth2ClientTest");
        assertEquals(OAuth2ClientType.CONFIDENTIAL.name(),
                node.at("/0/client_type").asText());
        assertNotNull(node.at("/0/client_description"));
        assertEquals(clientURL, node.at("/0/client_url").asText());
        assertEquals(clientRedirectUri,
                node.at("/0/client_redirect_uri").asText());
        assertNotNull(node.at("/0/registration_date"));
        assertEquals(defaultRefreshTokenExpiry,
                node.at("/0/refresh_token_expiry").asInt());
        assertTrue(node.at("/0/permitted").asBoolean());
        assertTrue(node.at("/0/source").isMissingNode());
    }

    // not ready until the behavior for (filterBy=null || filterBy.isEmpty) is set
    private void testListAllUserClients (String username) throws KustvaktException {
        // authorize
        String userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, "password");
        String code = requestAuthorizationCode(confidentialClientId, userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, this.clientSecret, code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken = node.at("/access_token").asText();
        
        node = listUserClients(username,null);
        assertEquals(2, node.size());
        
        testRevokeAllTokenViaSuperClient(confidentialClientId, userAuthHeader,
                accessToken);
    }
    
    @Test
    public void testListAuthorizedUserClients () throws KustvaktException {
        String username = "pearl";
        String password = "pwd";
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, password);
        // super client
        Response response = requestTokenWithPassword(superClientId,
                clientSecret, username, password);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // client 1
        String code = requestAuthorizationCode(publicClientId, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(publicClientId, "",
                code);
//        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken = node.at("/access_token").asText();
        // client 2
        code = requestAuthorizationCode(confidentialClientId, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        String refreshToken = node.at("/refresh_token").asText();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        testListAuthorizedClients(userAuthHeader);
        testListAuthorizedClientWithMultipleRefreshTokens(userAuthHeader);
        testListAuthorizedClientWithMultipleAccessTokens(userAuthHeader);
        testListWithClientsFromAnotherUser(userAuthHeader);
        // revoke client 1
        testRevokeAllTokenViaSuperClient(publicClientId, userAuthHeader,
                accessToken);
        // revoke client 2
        node = JsonUtils.readTree(response.readEntity(String.class));
        accessToken = node.at("/access_token").asText();
        refreshToken = node.at("/refresh_token").asText();
        testRevokeAllTokenViaSuperClient(confidentialClientId, userAuthHeader,
                accessToken);
        testRequestTokenWithRevokedRefreshToken(confidentialClientId,
                clientSecret, refreshToken);
    }

    private void testListAuthorizedClientWithMultipleRefreshTokens (
            String userAuthHeader) throws KustvaktException {
        // client 2
        String code = requestAuthorizationCode(confidentialClientId,
                userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        testListAuthorizedClients(userAuthHeader);
    }

    private void testListAuthorizedClientWithMultipleAccessTokens (
            String userAuthHeader) throws KustvaktException {
        // client 1
        String code = requestAuthorizationCode(publicClientId, userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, "", code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        testListAuthorizedClients(userAuthHeader);
    }

    private void testListWithClientsFromAnotherUser (String userAuthHeader)
            throws KustvaktException {
        String aaaAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("aaa", "pwd");
        // client 1
        String code = requestAuthorizationCode(publicClientId, aaaAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, "", code);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken1 = node.at("/access_token").asText();
        // client 2
        code = requestAuthorizationCode(confidentialClientId, aaaAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken2 = node.at("/access_token").asText();
        String refreshToken = node.at("/refresh_token").asText();
        testListAuthorizedClients(aaaAuthHeader);
        testListAuthorizedClients(userAuthHeader);
        testRevokeAllTokenViaSuperClient(publicClientId, aaaAuthHeader,
                accessToken1);
        testRevokeAllTokenViaSuperClient(confidentialClientId, aaaAuthHeader,
                accessToken2);
        testRequestTokenWithRevokedRefreshToken(confidentialClientId,
                clientSecret, refreshToken);
    }

    private void testRevokeAllTokenViaSuperClient (String clientId,
            String userAuthHeader, String accessToken)
            throws KustvaktException {
        // check token before revoking
        Response response = searchWithAccessToken(accessToken);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertTrue(node.at("/matches").size() > 0);
        Form form = getSuperClientForm();
        form.param("client_id", clientId);
        response = target().path(API_VERSION).path("oauth2").path("revoke")
                .path("super").path("all").request()
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(response.readEntity(String.class), "SUCCESS");
        response = searchWithAccessToken(accessToken);
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Access token is invalid");
    }
}
