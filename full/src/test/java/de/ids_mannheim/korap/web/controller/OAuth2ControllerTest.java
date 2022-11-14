package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 *
 */
public class OAuth2ControllerTest extends OAuth2TestBase {

    public String userAuthHeader;

    public OAuth2ControllerTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
    }

    @Test
    public void testAuthorizeConfidentialClient () throws KustvaktException {
        // with registered redirect URI
        Response response =
                requestAuthorizationCode("code", confidentialClientId, "",
                        "match_info search client_info", state, userAuthHeader);

        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());
        URI redirectUri = response.getLocation();
        MultivaluedMap<String, String> params =
                getQueryParamsFromURI(redirectUri);
        assertNotNull(params.getFirst("code"));
        assertEquals(state, params.getFirst("state"));
        assertEquals("match_info search client_info", params.getFirst("scope"));
    }

    @Test
    public void testAuthorizePublicClient () throws KustvaktException {
        // with registered redirect URI
        String code = requestAuthorizationCode(publicClientId, userAuthHeader);
        assertNotNull(code);
    }

    @Test
    public void testAuthorizeWithRedirectUri () throws KustvaktException {
        Response response =
                requestAuthorizationCode("code", publicClientId2,
                        "https://public.com/redirect", "", "", userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        URI redirectUri = response.getLocation();
        assertEquals("https", redirectUri.getScheme());
        assertEquals("public.com", redirectUri.getHost());
        assertEquals("/redirect", redirectUri.getPath());

        String[] queryParts = redirectUri.getQuery().split("&");
        assertTrue(queryParts[0].startsWith("code="));
        assertEquals("scope=match_info+search", queryParts[1]);
    }

    @Test
    public void testAuthorizeWithoutScope () throws KustvaktException {
        Response response = requestAuthorizationCode("code",
                confidentialClientId, "", "", "", userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        URI redirectUri = response.getLocation();
        assertEquals(redirectUri.getScheme(), "https");
        assertEquals(redirectUri.getHost(), "third.party.com");
        assertEquals(redirectUri.getPath(), "/confidential/redirect");

        String[] queryParts = redirectUri.getQuery().split("&");
        assertTrue(queryParts[0].startsWith("code="));
        assertEquals(queryParts[1], "scope=match_info+search");
    }

    @Test
    public void testAuthorizeMissingClientId () throws KustvaktException {
        Response response = requestAuthorizationCode("code", "", "", "",
                "", userAuthHeader);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("Missing parameters: client_id",
                node.at("/error_description").asText());
    }

    @Test
    public void testAuthorizeMissingRedirectUri () throws KustvaktException {
        Response response = requestAuthorizationCode("code",
                publicClientId2, "", "", state, userAuthHeader);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing parameter: redirect URI",
                node.at("/error_description").asText());
        assertEquals(state, node.at("/state").asText());
    }

    @Test
    public void testAuthorizeMissingResponseType() throws KustvaktException {
        Response response = requestAuthorizationCode("",
                confidentialClientId, "", "", "", userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        assertEquals("https://third.party.com/confidential/redirect?"
                + "error_description=Missing+parameters%3A+response_type&"
                + "error=invalid_request", response.getLocation().toString());
    }
    
    @Test
    public void testAuthorizeMissingResponseTypeWithoutClientId () throws KustvaktException {
        Response response = requestAuthorizationCode("",
                "", "", "", "", userAuthHeader);
        
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing parameters: response_type client_id",
                node.at("/error_description").asText());
    }

    @Test
    public void testAuthorizeInvalidClientId () throws KustvaktException {
        Response response = requestAuthorizationCode("code",
                "unknown-client-id", "", "", "", userAuthHeader);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_CLIENT, node.at("/error").asText());
        assertEquals("Unknown client: unknown-client-id",
                node.at("/error_description").asText());
    }

    @Test
    public void testAuthorizeDifferentRedirectUri () throws KustvaktException {
        String redirectUri = "https://different.uri/redirect";
        Response response = requestAuthorizationCode("code",
                confidentialClientId, redirectUri, "", state, userAuthHeader);
        testInvalidRedirectUri(response.readEntity(String.class), true,
                response.getStatus());
    }

    @Test
    public void testAuthorizeWithRedirectUriLocalhost ()
            throws KustvaktException {
        Response response =
                requestAuthorizationCode("code", publicClientId2,
                        "http://localhost:1410", "", state, userAuthHeader);
        testInvalidRedirectUri(response.readEntity(String.class), true,
                response.getStatus());    }

    @Test
    public void testAuthorizeWithRedirectUriFragment ()
            throws KustvaktException {
        Response response = requestAuthorizationCode("code",
                publicClientId2, "http://public.com/index.html#redirect", "",
                state, userAuthHeader);
        testInvalidRedirectUri(response.readEntity(String.class), true,
                response.getStatus());
    }

    @Test
    public void testAuthorizeInvalidRedirectUri () throws KustvaktException {
        // host not allowed by Apache URI Validator
        String redirectUri = "https://public.uri/redirect";
        Response response = requestAuthorizationCode("code",
                publicClientId2, redirectUri, "", state, userAuthHeader);
        testInvalidRedirectUri(response.readEntity(String.class), true,
                response.getStatus());
    }

    @Test
    public void testAuthorizeInvalidResponseType () throws KustvaktException {
        // without redirect URI in the request
        Response response = requestAuthorizationCode("string",
                confidentialClientId, "", "", state, userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        assertEquals("https://third.party.com/confidential/redirect?"
                + "error_description=Invalid+response_type+parameter+"
                + "value&state=thisIsMyState&" + "error=invalid_request",
                response.getLocation().toString());

        // with redirect URI, and no registered redirect URI
        response = requestAuthorizationCode("string", publicClientId2,
                "https://public.client.com/redirect", "", state,
                userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        assertEquals("https://public.client.com/redirect?error_description="
                + "Invalid+response_type+parameter+value&state=thisIsMyState&"
                + "error=invalid_request", response.getLocation().toString());

        // with different redirect URI
        String redirectUri = "https://different.uri/redirect";
        response = requestAuthorizationCode("string", confidentialClientId,
                redirectUri, "", state, userAuthHeader);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Invalid redirect URI",
                node.at("/error_description").asText());
        assertEquals(state, node.at("/state").asText());

        // without redirect URI in the request and no registered
        // redirect URI
        response = requestAuthorizationCode("string", publicClientId2, "", "",
                state, userAuthHeader);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing parameter: redirect URI",
                node.at("/error_description").asText());
        assertEquals(state, node.at("/state").asText());
    }

    @Test
    public void testAuthorizeInvalidScope () throws KustvaktException {
        String scope = "read_address";
        Response response = requestAuthorizationCode("code",
                confidentialClientId, "", scope, state, userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        assertEquals(
                "https://third.party.com/confidential/redirect?"
                        + "error_description=read_address+is+an+invalid+scope&"
                        + "state=thisIsMyState&error=invalid_scope",
                response.getLocation().toString());
    }

    @Test
    public void testAuthorizeUnsupportedTokenResponseType ()
            throws KustvaktException {
        Response response = requestAuthorizationCode("token",
                confidentialClientId, "", "", state, userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        assertEquals("https://third.party.com/confidential/redirect?"
                + "error_description=response_type+token+is+not+"
                + "supported&state=thisIsMyState&error=unsupported_"
                + "response_type", response.getLocation().toString());
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

        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertEquals(31536000, node.at("/expires_in").asInt());

        testRevokeToken(accessToken, publicClientId, null, ACCESS_TOKEN_TYPE);

        assertTrue(node.at("/refresh_token").isMissingNode());
    }

    @Test
    public void testRequestTokenAuthorizationConfidential ()
            throws KustvaktException {

        String scope = "search";
        Response response = requestAuthorizationCode("code",
                confidentialClientId, "", scope, state, userAuthHeader);
        MultivaluedMap<String, String> params =
                getQueryParamsFromURI(response.getLocation());
        String code = params.get("code").get(0);
        String scopes = params.get("scope").get(0);

        assertEquals(scopes, "search");

        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
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
        assertEquals(OAuthError.TokenResponse.INVALID_GRANT,
                node.at("/error").asText());
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
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
    }

    @Test
    public void testRequestTokenAuthorizationReplyAttack ()
            throws KustvaktException {
        String redirect_uri = "https://third.party.com/confidential/redirect";
        String scope = "search";

        Response response =
                requestAuthorizationCode("code", confidentialClientId,
                        redirect_uri, scope, state, userAuthHeader);
        MultivaluedMap<String, String> params =
                getQueryParamsFromURI(response.getLocation());
        String code = params.get("code").get(0);

        testRequestTokenAuthorizationInvalidClient(code);
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
        assertEquals(OAuthError.TokenResponse.INVALID_GRANT,
                node.at("/error").asText());
        assertEquals("Invalid authorization",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantConfidentialSuper ()
            throws KustvaktException {
        Response response =
                requestTokenWithDoryPassword(superClientId, clientSecret);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());

        String refresh = node.at("/refresh_token").asText();
        RefreshToken refreshToken =
                refreshTokenDao.retrieveRefreshToken(refresh);
        Set<AccessScope> scopes = refreshToken.getScopes();
        assertEquals(1, scopes.size());
        assertEquals("[all]", scopes.toString());
        
        testRefreshTokenExpiry(refresh);
    }

    @Test
    public void testRequestTokenPasswordGrantConfidentialNonSuper ()
            throws KustvaktException {
        Response response = requestTokenWithDoryPassword(
                confidentialClientId, clientSecret);
        String entity = response.readEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT,
                node.at("/error").asText());
        assertEquals("Password grant is not allowed for third party clients",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantPublic ()
            throws KustvaktException {
        Response response =
                requestTokenWithDoryPassword(publicClientId, "");
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

        Response response =
                target().path(API_VERSION).path("oauth2").path("token")
                        .request()
                        .header(HttpHeaders.AUTHORIZATION,
                                "Basic ZkNCYlFrQXlZekk0TnpVeE1nOnNlY3JldA==")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
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

        Response response =
                target().path(API_VERSION).path("oauth2").path("token")
                        .request()
                        .header(HttpHeaders.AUTHORIZATION,
                                "Basic ZkNCYlFrQXlZekk0TnpVeE1nOnNlY3JldA==")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantMissingClientSecret ()
            throws KustvaktException {
        Response response =
                requestTokenWithDoryPassword(confidentialClientId, "");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing parameter: client_secret",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantMissingClientId ()
            throws KustvaktException {
        Response response =
                requestTokenWithDoryPassword(null, clientSecret);
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing parameters: client_id",
                node.at("/error_description").asText());
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
        assertEquals(TokenType.BEARER.toString(),
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
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing parameters: client_secret",
                node.at("/error_description").asText());
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
        assertEquals(TokenType.BEARER.toString(),
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
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
    }

    @Test
    public void testRequestTokenUnsupportedGrant () throws KustvaktException {

        Form form = new Form();
        form.param("grant_type", "blahblah");

        Response response =
                target().path(API_VERSION).path("oauth2").path("token")
                        .request()
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .post(Entity.form(form));

        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("Invalid grant_type parameter value",
                node.get("error_description").asText());
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.get("error").asText());
    }

    private void testRequestRefreshTokenInvalidScope (String clientId,
            String refreshToken) throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.param("client_id", clientId);
        form.param("client_secret", clientSecret);
        form.param("refresh_token", refreshToken);
        form.param("scope", "search serialize_query");

        Response response =
                target().path(API_VERSION).path("oauth2").path("token")
                        .request()
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

        Response response =
                target().path(API_VERSION).path("oauth2").path("token")
                        .request()
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
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());

        assertTrue(!newRefreshToken.equals(refreshToken));

        testRequestTokenWithRevokedRefreshToken(clientId, clientSecret,
                refreshToken);

        testRevokeToken(newRefreshToken, clientId, clientSecret,
                REFRESH_TOKEN_TYPE);
        testRequestTokenWithRevokedRefreshToken(clientId, clientSecret,
                newRefreshToken);
    }

    private void testRequestRefreshTokenInvalidClient (String refreshToken)
            throws KustvaktException {
        Form form = new Form();
        form.param("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.param("client_id", "iBr3LsTCxOj7D2o0A5m");
        form.param("refresh_token", refreshToken);

        Response response =
                target().path(API_VERSION).path("oauth2").path("token")
                        .request()
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
                .path("token").path("list")
                .request()
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
        String code =
                requestAuthorizationCode(confidentialClientId, userAuthHeader);
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

        testRevokeToken(refreshToken1, superClientId, clientSecret,
                REFRESH_TOKEN_TYPE);
        testRevokeToken(node.at("/0/token").asText(), confidentialClientId,
                clientSecret, REFRESH_TOKEN_TYPE);
        testRevokeToken(node.at("/1/token").asText(), confidentialClientId2,
                clientSecret, REFRESH_TOKEN_TYPE);

        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(1, node.size());

        testRevokeTokenViaSuperClient(node.at("/0/token").asText(),
                userAuthHeader);
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(0, node.size());

        // try revoking a token belonging to another user
        // should not return any errors
        testRevokeTokenViaSuperClient(refreshToken5, userAuthHeader);
        node = requestTokenList(darlaAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(1, node.size());

        testRevokeTokenViaSuperClient(refreshToken5, darlaAuthHeader);
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

        testRevokeTokenViaSuperClient(accessToken1, userAuthHeader);
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

        testRevokeTokenViaSuperClient(accessToken2, userAuthHeader);
        node = requestTokenList(userAuthHeader, ACCESS_TOKEN_TYPE);
        assertEquals(0, node.size());
    }
    
    private void testRefreshTokenExpiry (String refreshToken)
            throws KustvaktException {
        RefreshToken token = refreshTokenDao.retrieveRefreshToken(refreshToken);
        ZonedDateTime expiry = token.getCreatedDate().plusYears(1);
        assertTrue(expiry.equals(token.getExpiryDate()));
    }
}
