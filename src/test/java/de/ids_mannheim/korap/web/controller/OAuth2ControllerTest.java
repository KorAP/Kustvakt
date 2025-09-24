package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.Set;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.nimbusds.oauth2.sdk.GrantType;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @author margaretha
 */
public class OAuth2ControllerTest extends OAuth2TestBase {

    @Autowired
    public FullConfiguration config;

    public String userAuthHeader;

    public OAuth2ControllerTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
    }

    @Test
    public void testRequestTokenAuthorizationPublic ()
            throws KustvaktException {
        String code = requestAuthorizationCode(publicClientId, userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, clientSecret, code);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        String accessToken = node.at("/access_token").asText();
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertEquals(31536000, node.at("/expires_in").asInt());
        revokeToken(accessToken, publicClientId, null, ACCESS_TOKEN_TYPE);
        assertTrue(node.at("/refresh_token").isMissingNode());
    }

    @Test
    public void testRequestTokenAuthorizationConfidential ()
            throws KustvaktException {
        String scope = "search";
        Response response = requestAuthorizationCode("code",
                confidentialClientId, "", scope, state, userAuthHeader);
        MultivaluedMap<String, String> params = getQueryParamsFromURI(
                response.getLocation());
        String code = params.get("code").get(0);

        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
        testRequestTokenWithUsedAuthorization(code);
        String refreshToken = node.at("/refresh_token").asText();
        testRefreshTokenExpiry(refreshToken);
        testRequestRefreshTokenInvalidScope(confidentialClientId, refreshToken);
        testRequestRefreshTokenInvalidClient(refreshToken);
        testRequestRefreshTokenInvalidRefreshToken(confidentialClientId);
        testRequestRefreshToken(confidentialClientId, clientSecret,
                refreshToken);
    }

    private void testRequestTokenWithUsedAuthorization (String code)
            throws KustvaktException {
        Response response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_GRANT, node.at("/error").asText());
        assertEquals("Invalid authorization",
            node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenInvalidAuthorizationCode ()
            throws KustvaktException {
        Response response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, "blahblah");
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
    }

    @Test
    public void testRequestTokenAuthorizationReplyAttack ()
            throws KustvaktException {
        String redirect_uri = "https://third.party.com/confidential/redirect";
        String scope = "search";
        Response response = requestAuthorizationCode("code",
                confidentialClientId, redirect_uri, scope, state,
                userAuthHeader);
        String code = parseAuthorizationCode(response);
        testRequestTokenAuthorizationInvalidClient(code);
        testRequestTokenAuthorizationMissingRedirectUri(code);
        testRequestTokenAuthorizationInvalidRedirectUri(code);
        testRequestTokenAuthorizationRevoked(code, redirect_uri);
    }

    private void testRequestTokenAuthorizationInvalidClient (String code)
            throws KustvaktException {
        Response response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, "wrong_secret", code);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_CLIENT, node.at("/error").asText());
    }

    private void testRequestTokenAuthorizationMissingRedirectUri (String code)
            throws KustvaktException {
        Response response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, "secret", code);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_GRANT, node.at("/error").asText());
        assertEquals("Missing redirect URI",
            node.at("/error_description").asText());
    }

    private void testRequestTokenAuthorizationInvalidRedirectUri (String code)
            throws KustvaktException {
        Form tokenForm = new Form();
        tokenForm.param("grant_type", "authorization_code");
        tokenForm.param("client_id", confidentialClientId);
        tokenForm.param("client_secret", "secret");
        tokenForm.param("code", code);
        tokenForm.param("redirect_uri", "https://blahblah.com");
        Response response = requestToken(tokenForm);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_GRANT, node.at("/error").asText());
    }

    private void testRequestTokenAuthorizationRevoked (String code, String uri)
            throws KustvaktException {
        Form tokenForm = new Form();
        tokenForm.param("grant_type", "authorization_code");
        tokenForm.param("client_id", confidentialClientId);
        tokenForm.param("client_secret", "secret");
        tokenForm.param("code", code);
        tokenForm.param("redirect_uri", uri);
        Response response = requestToken(tokenForm);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_GRANT, node.at("/error").asText());
        assertEquals("Invalid authorization",
            node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantConfidentialSuper ()
            throws KustvaktException {
        Response response = requestTokenWithDoryPassword(superClientId,
                clientSecret);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
        assertEquals("all", node.at("/scope").asText());
        String refresh = node.at("/refresh_token").asText();
        RefreshToken refreshToken = refreshTokenDao
                .retrieveRefreshToken(refresh);
        Set<AccessScope> scopes = refreshToken.getScopes();
        assertEquals(1, scopes.size());
        assertEquals("[all]", scopes.toString());
        testRefreshTokenExpiry(refresh);
    }

    @Test
    public void testRequestTokenPasswordGrantWithScope ()
            throws KustvaktException {
        String scope = "match_info search";
        Form form = new Form();
        form.param("grant_type", "password");
        form.param("client_id", superClientId);
        form.param("client_secret", clientSecret);
        form.param("username", "dory");
        form.param("password", "pwd");
        form.param("scope", scope);
        Response response = requestToken(form);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
        assertEquals(scope, node.at("/scope").asText());
        String refreshToken = node.at("/refresh_token").asText();
        testRequestRefreshTokenWithUnauthorizedScope(superClientId,
                clientSecret, refreshToken, "all");
        testRequestRefreshTokenWithScope(superClientId, clientSecret,
                refreshToken, "search");
    }

    @Test
    public void testRequestTokenPasswordGrantConfidentialNonSuper ()
            throws KustvaktException {
        Response response = requestTokenWithDoryPassword(confidentialClientId,
                clientSecret);
        String entity = response.readEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.UNAUTHORIZED_CLIENT,
                node.at("/error").asText());
        assertEquals("Password grant is not allowed for third party clients",
            node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantPublic ()
            throws KustvaktException {
        Response response = requestTokenWithDoryPassword(publicClientId, "");
        String entity = response.readEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.UNAUTHORIZED_CLIENT,
                node.at("/error").asText());
        assertEquals("Password grant is not allowed for third party clients",
            node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantAuthorizationHeader ()
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", "password");
        form.param("client_id", superClientId);
        form.param("username", "dory");
        form.param("password", "password");
        Response response = target().path(API_VERSION).path("oauth2")
                .path("token").request()
                .header(HttpHeaders.AUTHORIZATION,
                        "Basic ZkNCYlFrQXlZekk0TnpVeE1nOnNlY3JldA==")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }

    /**
     * In case, client_id is specified both in Authorization header
     * and request body, client_id in the request body is ignored.
     *
     * @throws KustvaktException
     */
    @Test
    public void testRequestTokenPasswordGrantDifferentClientIds ()
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", "password");
        form.param("client_id", "9aHsGW6QflV13ixNpez");
        form.param("username", "dory");
        form.param("password", "password");
        Response response = target().path(API_VERSION).path("oauth2")
                .path("token").request()
                .header(HttpHeaders.AUTHORIZATION,
                        "Basic ZkNCYlFrQXlZekk0TnpVeE1nOnNlY3JldA==")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantMissingClientSecret ()
            throws KustvaktException {
        Response response = requestTokenWithDoryPassword(confidentialClientId,
                "");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertNotNull(node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantEmptyClientSecret ()
            throws KustvaktException {
        Response response = requestTokenWithDoryPassword(confidentialClientId,
                "");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Missing parameter: client_secret",
            node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantMissingClientId ()
            throws KustvaktException {
        Response response = requestTokenWithDoryPassword(null, clientSecret);
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertNotNull(node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantEmptyClientId ()
            throws KustvaktException {
        Response response = requestTokenWithDoryPassword("", clientSecret);
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertNotNull(node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenClientCredentialsGrant ()
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", "client_credentials");
        form.param("client_id", confidentialClientId);
        form.param("client_secret", "secret");
        Response response = requestToken(form);
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        // length?
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }

    /**
     * Client credentials grant is only allowed for confidential
     * clients.
     */
    @Test
    public void testRequestTokenClientCredentialsGrantPublic ()
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", "client_credentials");
        form.param("client_id", publicClientId);
        form.param("client_secret", "");
        Response response = requestToken(form);
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertNotNull(node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenClientCredentialsGrantReducedScope ()
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", "client_credentials");
        form.param("client_id", confidentialClientId);
        form.param("client_secret", "secret");
        form.param("scope", "preferred_username client_info");
        Response response = requestToken(form);
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        // length?
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
        assertEquals("client_info", node.at("/scope").asText());
    }

    @Test
    public void testRequestTokenMissingGrantType () throws KustvaktException {
        Form form = new Form();
        Response response = requestToken(form);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
    }

    @Test
    public void testRequestTokenUnsupportedGrant () throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", "blahblah");
        Response response = target().path(API_VERSION).path("oauth2")
                .path("token").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.get("error_description").asText());
        assertEquals(OAuth2Error.INVALID_REQUEST, node.get("error").asText());
    }

    private void testRequestRefreshTokenInvalidScope (String clientId,
            String refreshToken) throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.param("client_id", clientId);
        form.param("client_secret", clientSecret);
        form.param("refresh_token", refreshToken);
        form.param("scope", "search serialize_query");
        Response response = target().path(API_VERSION).path("oauth2")
                .path("token").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_SCOPE, node.at("/error").asText());
    }

    private void testRequestRefreshToken (String clientId, String clientSecret,
            String refreshToken) throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.param("client_id", clientId);
        form.param("client_secret", clientSecret);
        form.param("refresh_token", refreshToken);
        Response response = target().path(API_VERSION).path("oauth2")
                .path("token").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        String newRefreshToken = node.at("/refresh_token").asText();
        assertNotNull(newRefreshToken);
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
        assertTrue(!newRefreshToken.equals(refreshToken));
        testRequestTokenWithRevokedRefreshToken(clientId, clientSecret,
                refreshToken);
        revokeToken(newRefreshToken, clientId, clientSecret,
                REFRESH_TOKEN_TYPE);
        testRequestTokenWithRevokedRefreshToken(clientId, clientSecret,
                newRefreshToken);
    }

    private void testRequestRefreshTokenWithUnauthorizedScope (String clientId,
            String clientSecret, String refreshToken, String scope)
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.param("client_id", clientId);
        form.param("client_secret", clientSecret);
        form.param("refresh_token", refreshToken);
        form.param("scope", scope);
        Response response = target().path(API_VERSION).path("oauth2")
                .path("token").request()
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_SCOPE, node.at("/error").asText());
    }

    private void testRequestRefreshTokenWithScope (String clientId,
            String clientSecret, String refreshToken, String scope)
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.param("client_id", clientId);
        form.param("client_secret", clientSecret);
        form.param("refresh_token", refreshToken);
        form.param("scope", scope);
        Response response = target().path(API_VERSION).path("oauth2")
                .path("token").request()
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        String newRefreshToken = node.at("/refresh_token").asText();
        assertNotNull(newRefreshToken);
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
        assertTrue(!newRefreshToken.equals(refreshToken));
        assertEquals(scope, node.at("/scope").asText());
    }

    private void testRequestRefreshTokenInvalidClient (String refreshToken)
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.param("client_id", "iBr3LsTCxOj7D2o0A5m");
        form.param("refresh_token", refreshToken);
        Response response = target().path(API_VERSION).path("oauth2")
                .path("token").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_CLIENT, node.at("/error").asText());
    }

    private void testRequestRefreshTokenInvalidRefreshToken (String clientId)
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.param("client_id", clientId);
        form.param("client_secret", clientSecret);
        form.param("refresh_token", "Lia8s8w8tJeZSBlaQDrYV8ion3l");
        Response response = target().path(API_VERSION).path("oauth2")
                .path("token").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_GRANT, node.at("/error").asText());
    }

    private JsonNode requestTokenList (String userAuthHeader, String tokenType,
            String clientId) throws KustvaktException {
        Form form = new Form();
        form.param("super_client_id", superClientId);
        form.param("super_client_secret", clientSecret);
        form.param("token_type", tokenType);
        if (clientId != null && !clientId.isEmpty()) {
            form.param("client_id", clientId);
        }
        Response response = target().path(API_VERSION).path("oauth2")
                .path("token").path("list").request()
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    private JsonNode requestTokenList (String userAuthHeader, String tokenType)
            throws KustvaktException {
        return requestTokenList(userAuthHeader, tokenType, null);
    }

    @Test
    public void testListRefreshTokenConfidentialClient ()
            throws KustvaktException {
        String username = "gurgle";
        String password = "pwd";
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, password);
        // super client
        Response response = requestTokenWithPassword(superClientId,
                clientSecret, username, password);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String refreshToken1 = node.at("/refresh_token").asText();
        // client 1
        String code = requestAuthorizationCode(confidentialClientId,
                userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // client 2
        code = requestAuthorizationCode(confidentialClientId2,
                clientRedirectUri, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId2, clientSecret, code, clientRedirectUri);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // list
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(2, node.size());
        assertEquals(confidentialClientId, node.at("/0/client_id").asText());
        assertEquals(confidentialClientId2, node.at("/1/client_id").asText());
        // client 1
        code = requestAuthorizationCode(confidentialClientId, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // another user
        String darlaAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("darla", "pwd");
        // test listing clients
        node = requestTokenList(darlaAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(0, node.size());
        // client 1
        code = requestAuthorizationCode(confidentialClientId, darlaAuthHeader);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        node = JsonUtils.readTree(response.readEntity(String.class));
        String refreshToken5 = node.at("/refresh_token").asText();
        // list all refresh tokens
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(3, node.size());
        // list refresh tokens from client 1
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE,
                confidentialClientId);
        assertEquals(2, node.size());
        revokeToken(refreshToken1, superClientId, clientSecret,
                REFRESH_TOKEN_TYPE);
        revokeToken(node.at("/0/token").asText(), confidentialClientId,
                clientSecret, REFRESH_TOKEN_TYPE);
        revokeToken(node.at("/1/token").asText(), confidentialClientId2,
                clientSecret, REFRESH_TOKEN_TYPE);
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(1, node.size());
        revokeTokenViaSuperClient(node.at("/0/token").asText(),
                userAuthHeader);
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(0, node.size());
        // try revoking a token belonging to another user
        // should not return any errors
        revokeTokenViaSuperClient(refreshToken5, userAuthHeader);
        node = requestTokenList(darlaAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(1, node.size());
        revokeTokenViaSuperClient(refreshToken5, darlaAuthHeader);
        node = requestTokenList(darlaAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(0, node.size());
    }

    @Test
    public void testListTokenPublicClient () throws KustvaktException {
        String username = "nemo";
        String password = "pwd";
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, password);
        // access token 1
        String code = requestAuthorizationCode(publicClientId, userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, "", code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken1 = node.at("/access_token").asText();
        // access token 2
        code = requestAuthorizationCode(publicClientId, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(publicClientId, "",
                code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken2 = node.at("/access_token").asText();
        // list access tokens
        node = requestTokenList(userAuthHeader, ACCESS_TOKEN_TYPE);
        assertEquals(2, node.size());
        // list refresh tokens
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(0, node.size());
        revokeTokenViaSuperClient(accessToken1, userAuthHeader);
        node = requestTokenList(userAuthHeader, ACCESS_TOKEN_TYPE);
        // System.out.println(node);
        assertEquals(1, node.size());
        assertEquals(accessToken2, node.at("/0/token").asText());
        assertTrue(node.at("/0/scope").size() > 0);
        assertNotNull(node.at("/0/created_date").asText());
        assertNotNull(node.at("/0/expires_in").asLong());
        assertNotNull(node.at("/0/user_authentication_time").asText());
        assertEquals(publicClientId, node.at("/0/client_id").asText());
        assertNotNull(node.at("/0/client_name").asText());
        assertNotNull(node.at("/0/client_description").asText());
        assertNotNull(node.at("/0/client_url").asText());
        revokeTokenViaSuperClient(accessToken2, userAuthHeader);
        node = requestTokenList(userAuthHeader, ACCESS_TOKEN_TYPE);
        assertEquals(0, node.size());
    }

    private void testRefreshTokenExpiry (String refreshToken)
            throws KustvaktException {
        RefreshToken token = refreshTokenDao.retrieveRefreshToken(refreshToken);
        ZonedDateTime expiry = token.getCreatedDate()
                .plusSeconds(config.getRefreshTokenLongExpiry());
        assertTrue(expiry.equals(token.getExpiryDate()));
    }
}
