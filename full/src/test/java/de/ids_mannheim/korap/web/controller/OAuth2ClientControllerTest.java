package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ContainerRequest;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

/**
 * @author margaretha
 *
 */
public class OAuth2ClientControllerTest extends OAuth2TestBase {

    private String username = "OAuth2ClientControllerTest";
    private String userAuthHeader;

    public OAuth2ClientControllerTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
    }

    private void checkWWWAuthenticateHeader (ClientResponse response) {
        Set<Entry<String, List<String>>> headers =
                response.getHeaders().entrySet();

        for (Entry<String, List<String>> header : headers) {
            if (header.getKey().equals(ContainerRequest.WWW_AUTHENTICATE)) {
                assertEquals("Basic realm=\"Kustvakt\"",
                        header.getValue().get(0));
            }
        }
    }
    
    private ClientResponse registerClient (String username, 
            OAuth2ClientJson json) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        return resource().path(API_VERSION).path("oauth2").path("client")
                .path("register")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);
    }

    private ClientResponse registerConfidentialClient ()
            throws KustvaktException {

        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2ClientTest");
        json.setType(OAuth2ClientType.CONFIDENTIAL);
        json.setUrl("http://example.client.com");
        json.setRedirectURI("https://example.client.com/redirect");
        json.setDescription("This is a confidential test client.");

        return registerClient(username, json);
    }

    private JsonNode retrieveClientInfo (String clientId, String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path(clientId)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    @Test
    public void testRetrieveClientInfo () throws KustvaktException {
        // public client
        JsonNode clientInfo = retrieveClientInfo(publicClientId, "system");
        assertEquals(publicClientId, clientInfo.at("/id").asText());
        assertEquals("third party client", clientInfo.at("/name").asText());
        assertNotNull(clientInfo.at("/description"));
        assertNotNull(clientInfo.at("/url"));
        assertEquals("PUBLIC", clientInfo.at("/type").asText());
        assertEquals("system", clientInfo.at("/registered_by").asText());

        // confidential client
        clientInfo = retrieveClientInfo(confidentialClientId, "system");
        assertEquals(confidentialClientId, clientInfo.at("/id").asText());
        assertEquals("non super confidential client", clientInfo.at("/name").asText());
        assertNotNull(clientInfo.at("/url"));
        assertEquals(false,clientInfo.at("/is_super").asBoolean());
        assertEquals("CONFIDENTIAL", clientInfo.at("/type").asText());
        
        // super client
        clientInfo = retrieveClientInfo(superClientId, "system");
        assertEquals(superClientId, clientInfo.at("/id").asText());
        assertEquals("super confidential client", clientInfo.at("/name").asText());
        assertNotNull(clientInfo.at("/url"));
        assertEquals("CONFIDENTIAL", clientInfo.at("/type").asText());
        assertTrue(clientInfo.at("/is_super").asBoolean());
    }
    
    @Test
    public void testRegisterConfidentialClient () throws KustvaktException {
        ClientResponse response = registerConfidentialClient();
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();
        assertNotNull(clientId);
        assertNotNull(clientSecret);

        assertFalse(clientId.contains("a"));
            
        testResetConfidentialClientSecret(clientId, clientSecret);
        testDeregisterConfidentialClient(clientId);
    }

    @Test
    public void testRegisterClientNameTooShort ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("R");
        json.setType(OAuth2ClientType.PUBLIC);

        ClientResponse response = registerClient(username, json);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("client_name must contain at least 3 characters",
                node.at("/error_description").asText());
        assertEquals("invalid_request",
                node.at("/error").asText());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testRegisterClientMissingDescription ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("R client");
        json.setType(OAuth2ClientType.PUBLIC);

        ClientResponse response = registerClient(username, json);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("client_description is null",
                node.at("/error_description").asText());
        assertEquals("invalid_request",
                node.at("/error").asText());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testRegisterPublicClient () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2PublicClient");
        json.setType(OAuth2ClientType.PUBLIC);
        json.setUrl("http://test.public.client.com");
        json.setRedirectURI("https://test.public.client.com/redirect");
        json.setDescription("This is a public test client.");

        ClientResponse response = registerClient(username, json);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());

        testResetPublicClientSecret(clientId);
        testAccessTokenAfterDeregistration(clientId, null,null);
    }
    
    @Test
    public void testRegisterClientUsingPlainJson ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException, IOException {

        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("json/oauth2_public_client.json");
        String json = IOUtils.toString(is, Charset.defaultCharset());

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("register")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(username,
                                        "password"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());

        testResetPublicClientSecret(clientId);
        testAccessTokenAfterDeregistration(clientId, null, null);
    }
    
    @Test
    public void testRegisterDesktopApp () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2DesktopClient");
        json.setType(OAuth2ClientType.PUBLIC);
        json.setDescription("This is a desktop test client.");

        ClientResponse response = registerClient(username, json);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());

        testDeregisterPublicClientMissingUserAuthentication(clientId);
        testDeregisterPublicClientMissingId();
        testDeregisterPublicClient(clientId,username);
    }

    @Test
    public void testRegisterMultipleDesktopApps () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        // First client
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2DesktopClient1");
        json.setType(OAuth2ClientType.PUBLIC);
        json.setDescription("This is a desktop test client.");

        ClientResponse response = registerClient(username, json);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId1 = node.at("/client_id").asText();
        assertNotNull(clientId1);
        assertTrue(node.at("/client_secret").isMissingNode());

        // Second client
        json = new OAuth2ClientJson();
        json.setName("OAuth2DesktopClient2");
        json.setType(OAuth2ClientType.PUBLIC);
        json.setDescription("This is another desktop test client.");

        response = registerClient(username, json);

        entity = response.getEntity(String.class);
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

        String code = requestAuthorizationCode(clientId, "", null,
                userAuthHeader, redirectUri);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                clientId, clientSecret, code, redirectUri);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken = node.at("/access_token").asText();

        response = searchWithAccessToken(accessToken);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        code = requestAuthorizationCode(clientId, "", null, userAuthHeader,
                redirectUri);
        testDeregisterPublicClient(clientId, username);

        response = requestTokenWithAuthorizationCodeAndForm(clientId,
                clientSecret, code, redirectUri);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(OAuth2Error.INVALID_CLIENT.toString(),
                node.at("/error").asText());

        response = searchWithAccessToken(accessToken);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is invalid",
                node.at("/errors/0/1").asText());
    }

    private void testDeregisterPublicClientMissingUserAuthentication (
            String clientId) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("deregister").path(clientId)
                .delete(ClientResponse.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    private void testDeregisterPublicClientMissingId ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("deregister")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
    }

    private void testDeregisterPublicClient (String clientId, String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("deregister").path(clientId)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testDeregisterConfidentialClient (String clientId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("deregister").path(clientId)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testResetPublicClientSecret (String clientId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("reset")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Operation is not allowed for public clients",
                node.at("/error_description").asText());
    }

    private String testResetConfidentialClientSecret (String clientId,
            String clientSecret) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("reset")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(clientId, node.at("/client_id").asText());

        String newClientSecret = node.at("/client_secret").asText();
        assertTrue(!clientSecret.equals(newClientSecret));

        return newClientSecret;
    }

    @Test
    public void testUpdateClientPrivilege () throws KustvaktException {
        // register a client
        ClientResponse response = registerConfidentialClient();
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();

        // request an access token
        String clientAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(clientId, clientSecret);
        String code = requestAuthorizationCode(clientId, clientSecret, null,
                userAuthHeader);
        node = requestTokenWithAuthorizationCodeAndHeader(clientId, code,
                clientAuthHeader);
        String accessToken = node.at("/access_token").asText();

        testAccessTokenAfterUpgradingClient(clientId, accessToken);
        testAccessTokenAfterDegradingSuperClient(clientId, accessToken);

        testDeregisterConfidentialClient(clientId);
    }

    // old access tokens retain their scopes
    private void testAccessTokenAfterUpgradingClient (String clientId,
            String accessToken) throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);
        form.add("super", "true");

        updateClientPrivilege(form);
        JsonNode node = retrieveClientInfo(clientId, "admin");
        assertTrue(node.at("/is_super").asBoolean());

        // list vc
        ClientResponse response = resource().path(API_VERSION).path("vc")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Scope vc_info is not authorized",
                node.at("/errors/0/1").asText());

        // search
        response = searchWithAccessToken(accessToken);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testAccessTokenAfterDegradingSuperClient (String clientId,
            String accessToken) throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);
        form.add("super", "false");

        updateClientPrivilege(form);
        JsonNode node = retrieveClientInfo(clientId, username);
        assertTrue(node.at("/isSuper").isMissingNode());

        ClientResponse response = searchWithAccessToken(accessToken);
        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        String entity = response.getEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is invalid",
                node.at("/errors/0/1").asText());
    }

    private void requestAuthorizedClientList (String userAuthHeader)
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", superClientId);
        form.add("client_secret", clientSecret);
        form.add("authorized_only", "true");

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("list")
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
//        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
        assertEquals(confidentialClientId, node.at("/0/client_id").asText());
        assertEquals(publicClientId, node.at("/1/client_id").asText());
        
        assertEquals("non super confidential client",node.at("/0/client_name").asText());
        assertEquals("CONFIDENTIAL",node.at("/0/client_type").asText());
        assertFalse(node.at("/0/client_url").isMissingNode());
        assertFalse(node.at("/0/client_description").isMissingNode());
    }

    @Test
    public void testListUserClients () throws KustvaktException {
        String username = "pearl";
        String password = "pwd";
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, password);

        // super client
        ClientResponse response = requestTokenWithPassword(superClientId,
                clientSecret, username, password);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // client 1
        String code = requestAuthorizationCode(publicClientId, "",
                null, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(publicClientId, "",
                code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken = node.at("/access_token").asText();

        // client 2
        code = requestAuthorizationCode(confidentialClientId, clientSecret,
                null, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        String refreshToken = node.at("/refresh_token").asText();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        requestAuthorizedClientList(userAuthHeader);
        testListAuthorizedClientWithMultipleRefreshTokens(userAuthHeader);
        testListAuthorizedClientWithMultipleAccessTokens(userAuthHeader);
        testListWithClientsFromAnotherUser(userAuthHeader);
        
        // revoke client 1
        testRevokeAllTokenViaSuperClient(publicClientId, userAuthHeader,
                accessToken);
        
        // revoke client 2
        node = JsonUtils.readTree(response.getEntity(String.class));
        accessToken = node.at("/access_token").asText();
        refreshToken = node.at("/refresh_token").asText();
        testRevokeAllTokenViaSuperClient(confidentialClientId, userAuthHeader,
                accessToken);
        testRequestTokenWithRevokedRefreshToken(confidentialClientId, clientSecret,
                refreshToken);
    }

    private void testListAuthorizedClientWithMultipleRefreshTokens (
            String userAuthHeader) throws KustvaktException {
        // client 2
        String code = requestAuthorizationCode(confidentialClientId, clientSecret,
                null, userAuthHeader);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        requestAuthorizedClientList(userAuthHeader);
    }
    
    private void testListAuthorizedClientWithMultipleAccessTokens (
            String userAuthHeader) throws KustvaktException {
        // client 1
        String code = requestAuthorizationCode(publicClientId, "",
                null, userAuthHeader);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, "", code);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        requestAuthorizedClientList(userAuthHeader);
    }
    
    private void testListWithClientsFromAnotherUser (
            String userAuthHeader) throws KustvaktException {

        String aaaAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("aaa", "pwd");
        
        // client 1
        String code = requestAuthorizationCode(publicClientId, "",
                null, aaaAuthHeader);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, "", code);
        
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken1 = node.at("/access_token").asText();
        
        // client 2
        code = requestAuthorizationCode(confidentialClientId, clientSecret,
                null, aaaAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);

        node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken2 = node.at("/access_token").asText();
        String refreshToken = node.at("/refresh_token").asText();

        requestAuthorizedClientList(aaaAuthHeader);
        requestAuthorizedClientList(userAuthHeader);

        testRevokeAllTokenViaSuperClient(publicClientId, aaaAuthHeader,
                accessToken1);
        testRevokeAllTokenViaSuperClient(confidentialClientId, aaaAuthHeader,
                accessToken2);
        testRequestTokenWithRevokedRefreshToken(confidentialClientId, clientSecret,
                refreshToken);
    }

    private void testRevokeAllTokenViaSuperClient (String clientId,
            String userAuthHeader, String accessToken)
            throws KustvaktException {
        // check token before revoking
        ClientResponse response = searchWithAccessToken(accessToken);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertTrue(node.at("/matches").size() > 0);

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);
        form.add("super_client_id", superClientId);
        form.add("super_client_secret", clientSecret);

        response = resource().path(API_VERSION).path("oauth2").path("revoke")
                .path("super").path("all")
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("SUCCESS", response.getEntity(String.class));

        response = searchWithAccessToken(accessToken);
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is invalid",
                node.at("/errors/0/1").asText());
    }
    
    @Test
    public void testListRegisteredClients ()
            throws KustvaktException {
        
        String clientName = "OAuth2DoryClient";
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName(clientName);
        json.setType(OAuth2ClientType.PUBLIC);
        json.setDescription("This is dory client.");

        registerClient("dory", json);
        
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", superClientId);
        form.add("client_secret", clientSecret);

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("list")
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals(1, node.size());
        assertEquals(clientName, node.at("/0/client_name").asText());
        assertEquals(OAuth2ClientType.PUBLIC.name(), node.at("/0/client_type").asText());
        String clientId = node.at("/0/client_id").asText();
        testDeregisterPublicClient(clientId, "dory");
    }
}
