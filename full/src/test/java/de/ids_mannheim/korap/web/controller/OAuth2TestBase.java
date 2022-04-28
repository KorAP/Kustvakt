package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.RefreshTokenDao;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

/**
 * Provides common methods and variables for OAuth2 tests,
 * and does not run any test.
 * 
 * @author margaretha
 *
 */
public abstract class OAuth2TestBase extends SpringJerseyTest {

    @Autowired
    protected RefreshTokenDao refreshTokenDao;

    protected String publicClientId = "8bIDtZnH6NvRkW2Fq";
    // without registered redirect URI
    protected String publicClientId2 = "nW5qM63Rb2a7KdT9L";
    protected String confidentialClientId = "9aHsGW6QflV13ixNpez";
    protected String confidentialClientId2 = "52atrL0ajex_3_5imd9Mgw";
    protected String superClientId = "fCBbQkAyYzI4NzUxMg";
    protected String clientSecret = "secret";
    protected String state = "thisIsMyState";

    public static String ACCESS_TOKEN_TYPE = "access_token";
    public static String REFRESH_TOKEN_TYPE = "refresh_token";

    protected int defaultRefreshTokenExpiry = TimeUtils.convertTimeToSeconds("365D");
    
    protected String clientURL = "http://example.client.com";
    protected String clientRedirectUri = "https://example.client.com/redirect";

    protected MultivaluedMap<String, String> getQueryParamsFromURI (URI uri) {
        return UriComponent.decodeQuery(uri, true);
    };

    protected MultivaluedMap<String, String> getSuperClientForm () {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("super_client_id", superClientId);
        form.add("super_client_secret", clientSecret);
        return form;
    }

    protected ClientResponse requestAuthorizationCode (String responseType,
            String clientId, String redirectUri, String scope, String state,
            String authHeader) throws KustvaktException {

        return resource().path(API_VERSION).path("oauth2").path("authorize")
                .queryParam("response_type", responseType)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope).queryParam("state", state)
                .header(Attributes.AUTHORIZATION, authHeader)
                .get(ClientResponse.class);
    }

    protected String requestAuthorizationCode (String clientId,
            String authHeader) throws KustvaktException {

        ClientResponse response = requestAuthorizationCode("code", clientId, "",
                "", "", authHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());
        URI redirectUri = response.getLocation();

        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(redirectUri).build().getQueryParams();
        return params.getFirst("code");
    }

    protected String requestAuthorizationCode (String clientId,
            String redirect_uri, String authHeader) throws KustvaktException {
        ClientResponse response = requestAuthorizationCode("code", clientId,
                redirect_uri, "", "", authHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());
        URI redirectUri = response.getLocation();

        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(redirectUri).build().getQueryParams();
        return params.getFirst("code");
    }

    protected ClientResponse requestToken (MultivaluedMap<String, String> form)
            throws KustvaktException {
        return resource().path(API_VERSION).path("oauth2").path("token")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    // client credentials as form params
    protected ClientResponse requestTokenWithAuthorizationCodeAndForm (
            String clientId, String clientSecret, String code)
            throws KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);

        return resource().path(API_VERSION).path("oauth2").path("token")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    protected ClientResponse requestTokenWithAuthorizationCodeAndForm (
            String clientId, String clientSecret, String code,
            String redirectUri) throws KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        if (redirectUri != null) {
            form.add("redirect_uri", redirectUri);
        }

        return resource().path(API_VERSION).path("oauth2").path("token")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    // client credentials in authorization header
    protected JsonNode requestTokenWithAuthorizationCodeAndHeader (
            String clientId, String code, String authHeader)
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("code", code);

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("token").header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    protected ClientResponse requestTokenWithDoryPassword (String clientId,
            String clientSecret) throws KustvaktException {
        return requestTokenWithPassword(clientId, clientSecret, "dory",
                "password");
    }

    protected ClientResponse requestTokenWithPassword (String clientId,
            String clientSecret, String username, String password)
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", username);
        form.add("password", password);

        return requestToken(form);
    }

    protected void testRequestTokenWithRevokedRefreshToken (String clientId,
            String clientSecret, String refreshToken) throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        if (clientSecret != null) {
            form.add("client_secret", clientSecret);
        }

        ClientResponse response =
                resource().path(API_VERSION).path("oauth2").path("token")
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_GRANT, node.at("/error").asText());
        assertEquals("Refresh token has been revoked",
                node.at("/error_description").asText());
    }

    protected ClientResponse registerClient (String username,
            OAuth2ClientJson json) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        return resource().path(API_VERSION).path("oauth2").path("client")
                .path("register")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(username,
                                        "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);
    }

    protected ClientResponse registerConfidentialClient (String username)
            throws KustvaktException {

        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2ClientTest");
        json.setType(OAuth2ClientType.CONFIDENTIAL);
        json.setUrl(clientURL);
        json.setRedirectURI(clientRedirectUri);
        json.setDescription("This is a confidential test client.");

        return registerClient(username, json);
    }

    protected void testConfidentialClientInfo (String clientId, String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        JsonNode clientInfo = retrieveClientInfo(clientId, username);
        assertEquals(clientId, clientInfo.at("/id").asText());
        assertEquals("OAuth2ClientTest", clientInfo.at("/name").asText());
        assertEquals(OAuth2ClientType.CONFIDENTIAL.name(),
                clientInfo.at("/type").asText());
        assertEquals(username, clientInfo.at("/registered_by").asText());
        assertEquals(clientURL, clientInfo.at("/url").asText());
        assertEquals(clientRedirectUri,
                clientInfo.at("/redirect_uri").asText());
        // 31536000 seconds
        assertEquals(TimeUtils.convertTimeToSeconds("365D"),
                clientInfo.at("/refresh_token_expiry").asInt());
        assertNotNull(clientInfo.at("/description"));
        assertNotNull(clientInfo.at("/registration_date"));
        assertTrue(clientInfo.at("/permitted").asBoolean());
        assertTrue(clientInfo.at("/source").isMissingNode());

    }

    protected void deregisterConfidentialClient (String username,
            String clientId) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("deregister").path(clientId)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    protected JsonNode retrieveClientInfo (String clientId, String username)
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

    protected ClientResponse searchWithAccessToken (String accessToken) {
        return resource().path(API_VERSION).path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
    }

    protected void testRevokeTokenViaSuperClient (String token,
            String userAuthHeader) {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("token", token);
        form.add("super_client_id", superClientId);
        form.add("super_client_secret", clientSecret);

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("revoke").path("super")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .header(Attributes.AUTHORIZATION, userAuthHeader).entity(form)
                .post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("SUCCESS", response.getEntity(String.class));
    }

    protected void testRevokeToken (String token, String clientId,
            String clientSecret, String tokenType) {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("token_type", tokenType);
        form.add("token", token);
        form.add("client_id", clientId);
        if (clientSecret != null) {
            form.add("client_secret", clientSecret);
        }

        ClientResponse response =
                resource().path(API_VERSION).path("oauth2").path("revoke")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("SUCCESS", response.getEntity(String.class));
    }

    protected JsonNode listUserRegisteredClients (String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("list")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pwd"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(getSuperClientForm()).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        return JsonUtils.readTree(entity);
    }
    
    protected void testInvalidRedirectUri (String entity, boolean includeState,
            int status) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Invalid redirect URI",
                node.at("/error_description").asText());
        if (includeState) {
            assertEquals(state, node.at("/state").asText());
        }

        assertEquals(Status.BAD_REQUEST.getStatusCode(), status);
    }
}
