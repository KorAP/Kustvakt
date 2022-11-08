package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
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
import com.sun.jersey.api.client.UniformInterfaceException;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.uri.UriComponent;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
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

    protected Form getSuperClientForm () {
        Form form = new Form();
        form.param("super_client_id", superClientId);
        form.param("super_client_secret", clientSecret);
        return form;
    }

    protected Response requestAuthorizationCode (String responseType,
            String clientId, String redirectUri, String scope, String state,
            String authHeader) throws KustvaktException {

        WebTarget request =
                target().path(API_VERSION).path("oauth2").path("authorize");
        
        if (!responseType.isEmpty()) {
            request = request.queryParam("response_type", responseType);
        }
        if (!clientId.isEmpty()) {
            request = request.queryParam("client_id", clientId);
        }
        if (!redirectUri.isEmpty()) {
            request = request.queryParam("redirect_uri", redirectUri);
        }
        if (!scope.isEmpty()) {
            request = request.queryParam("scope", scope);
        }
        if (!state.isEmpty()) {
            request = request.queryParam("state", state);
        }
        
        return request.request().header(Attributes.AUTHORIZATION, authHeader)
                .get();
    }

    protected String requestAuthorizationCode (String clientId,
            String authHeader) throws KustvaktException {

        Response response = requestAuthorizationCode("code", clientId, "",
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
        Response response = requestAuthorizationCode("code", clientId,
                redirect_uri, "", "", authHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());
        URI redirectUri = response.getLocation();

        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(redirectUri).build().getQueryParams();
        return params.getFirst("code");
    }

    protected Response requestToken (Form form)
            throws KustvaktException {
        return target().path(API_VERSION).path("oauth2").path("token")
                .request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
               .post(Entity.form(form));
    }

    // client credentials as form params
    protected Response requestTokenWithAuthorizationCodeAndForm (
            String clientId, String clientSecret, String code)
            throws KustvaktException {

        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("client_id", clientId);
        form.param("client_secret", clientSecret);
        form.param("code", code);

        return target().path(API_VERSION).path("oauth2").path("token")
                .request()
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
    }

    protected Response requestTokenWithAuthorizationCodeAndForm (
            String clientId, String clientSecret, String code,
            String redirectUri) throws KustvaktException {

        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("client_id", clientId);
        form.param("client_secret", clientSecret);
        form.param("code", code);
        if (redirectUri != null) {
            form.param("redirect_uri", redirectUri);
        }

        return target().path(API_VERSION).path("oauth2").path("token")
                .request()
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
    }

    // client credentials in authorization header
    protected JsonNode requestTokenWithAuthorizationCodeAndHeader (
            String clientId, String code, String authHeader)
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("client_id", clientId);
        form.param("code", code);

        Response response = target().path(API_VERSION).path("oauth2")
                .path("token")
                .request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));

        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    protected Response requestTokenWithDoryPassword (String clientId,
            String clientSecret) throws KustvaktException {
        return requestTokenWithPassword(clientId, clientSecret, "dory",
                "password");
    }

    protected Response requestTokenWithPassword (String clientId,
            String clientSecret, String username, String password)
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", "password");
        form.param("client_id", clientId);
        form.param("client_secret", clientSecret);
        form.param("username", username);
        form.param("password", password);

        return requestToken(form);
    }

    protected void testRequestTokenWithRevokedRefreshToken (String clientId,
            String clientSecret, String refreshToken) throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.param("client_id", clientId);
        form.param("client_secret", clientSecret);
        form.param("refresh_token", refreshToken);
        if (clientSecret != null) {
            form.param("client_secret", clientSecret);
        }

        Response response =
                target().path(API_VERSION).path("oauth2").path("token")
                        .request()
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .post(Entity.form(form));

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_GRANT, node.at("/error").asText());
        assertEquals("Refresh token has been revoked",
                node.at("/error_description").asText());
    }

    protected Response registerClient (String username,
            OAuth2ClientJson json) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        return target().path(API_VERSION).path("oauth2").path("client")
                .path("register")
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(username,
                                        "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(Entity.json(json));
    }

    protected Response registerConfidentialClient (String username)
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
        assertEquals(clientId, clientInfo.at("/client_id").asText());
        assertEquals("OAuth2ClientTest", clientInfo.at("/client_name").asText());
        assertEquals(OAuth2ClientType.CONFIDENTIAL.name(),
                clientInfo.at("/client_type").asText());
        assertEquals(username, clientInfo.at("/registered_by").asText());
        assertEquals(clientURL, clientInfo.at("/client_url").asText());
        assertEquals(clientRedirectUri,
                clientInfo.at("/client_redirect_uri").asText());
        // 31536000 seconds
        assertEquals(defaultRefreshTokenExpiry,
                clientInfo.at("/refresh_token_expiry").asInt());
        assertNotNull(clientInfo.at("/description"));
        assertNotNull(clientInfo.at("/registration_date"));
        assertTrue(clientInfo.at("/permitted").asBoolean());
        assertTrue(clientInfo.at("/source").isMissingNode());

    }

    protected void deregisterClient (String username,
            String clientId) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path("deregister").path(clientId)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    protected JsonNode retrieveClientInfo (String clientId, String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path(clientId)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    protected Response searchWithAccessToken (String accessToken) {
        return target().path(API_VERSION).path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();
    }
    
    protected void testSearchWithOAuth2Token (String accessToken)
            throws KustvaktException, IOException {
        Response response = searchWithAccessToken(accessToken);
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals(25, node.at("/matches").size());
    }
    
    protected void testSearchWithRevokedAccessToken (String accessToken)
            throws KustvaktException {
        Response response = searchWithAccessToken(accessToken);
        String entity = response.readEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is invalid",
                node.at("/errors/0/1").asText());
    }


    protected void testRevokeTokenViaSuperClient (String token,
            String userAuthHeader) {
        Form form = new Form();
        form.param("token", token);
        form.param("super_client_id", superClientId);
        form.param("super_client_secret", clientSecret);

        Response response = target().path(API_VERSION).path("oauth2")
                .path("revoke").path("super")
                .request()
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .post(Entity.form(form));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("SUCCESS", response.readEntity(String.class));
    }

    protected void testRevokeToken (String token, String clientId,
            String clientSecret, String tokenType) {
        Form form = new Form();
        form.param("token_type", tokenType);
        form.param("token", token);
        form.param("client_id", clientId);
        if (clientSecret != null) {
            form.param("client_secret", clientSecret);
        }

        Response response =
                target().path(API_VERSION).path("oauth2").path("revoke")
                        .request()
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .post(Entity.form(form));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("SUCCESS", response.readEntity(String.class));
    }

    protected JsonNode listUserRegisteredClients (String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Form form = getSuperClientForm();
        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path("list")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pwd"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
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
