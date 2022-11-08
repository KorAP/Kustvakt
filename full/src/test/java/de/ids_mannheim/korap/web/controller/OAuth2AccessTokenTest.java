package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.junit.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class OAuth2AccessTokenTest extends OAuth2TestBase {

    private String userAuthHeader;
    private String clientAuthHeader;

    public OAuth2AccessTokenTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
        clientAuthHeader =
                HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(
                        confidentialClientId, clientSecret);
    }

    @Test
    public void testScopeWithSuperClient () throws KustvaktException {
        Response response =
                requestTokenWithDoryPassword(superClientId, clientSecret);

        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals("all", node.at("/scope").asText());
        String accessToken = node.at("/access_token").asText();

        // test list user group
        response = target().path(API_VERSION).path("group")
                .request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(2, node.size());
    }

    @Test
    public void testCustomScope () throws KustvaktException {
        Response response =
                requestAuthorizationCode("code", confidentialClientId, "",
                        OAuth2Scope.VC_INFO.toString(), "", userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());
        URI redirectUri = response.getLocation();
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(redirectUri).build().getQueryParams();
        String code = params.getFirst("code");
        
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));

        String token = node.at("/access_token").asText();
        assertTrue(node.at("/scope").asText()
                .contains(OAuth2Scope.VC_INFO.toString()));

        // test list vc using the token
        response = target().path(API_VERSION).path("vc")
                .request()
                .header(Attributes.AUTHORIZATION, "Bearer " + token)
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(4, node.size());
    }

    @Test
    public void testDefaultScope () throws KustvaktException, IOException {
        String code = requestAuthorizationCode(confidentialClientId, userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken = node.at("/access_token").asText();
        testScopeNotAuthorized(accessToken);
        testScopeNotAuthorize2(accessToken);
        testSearchWithOAuth2Token(accessToken);
    }

    private void testScopeNotAuthorized (String accessToken)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Scope vc_info is not authorized",
                node.at("/errors/0/1").asText());
    }

    private void testScopeNotAuthorize2 (String accessToken)
            throws KustvaktException {
        Response response =
                target().path(API_VERSION).path("vc").path("access")
                        .request()
                        .header(Attributes.AUTHORIZATION,
                                "Bearer " + accessToken)
                        .get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Scope vc_access_info is not authorized",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testSearchWithUnknownToken ()
            throws KustvaktException, IOException {
        Response response =
                searchWithAccessToken("ljsa8tKNRSczJhk20öhq92zG8z350");

        assertEquals(Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is invalid",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testRevokeAccessTokenConfidentialClient ()
            throws KustvaktException {
        String code = requestAuthorizationCode(confidentialClientId,
                userAuthHeader);
        JsonNode node = requestTokenWithAuthorizationCodeAndHeader(
                confidentialClientId, code, clientAuthHeader);

        String accessToken = node.at("/access_token").asText();
        Form form = new Form();
        form.param("token", accessToken);
        form.param("client_id", confidentialClientId);
        form.param("client_secret", "secret");

        Response response = target().path(API_VERSION).path("oauth2").path("revoke")
                .request()
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testSearchWithRevokedAccessToken(accessToken);
    }
    
    @Test
    public void testRevokeAccessTokenPublicClientViaSuperClient()
            throws KustvaktException {
        String code = requestAuthorizationCode(publicClientId,
                userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, "", code);
        
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken = node.at("/access_token").asText();
        testRevokeTokenViaSuperClient(accessToken, userAuthHeader);
        testSearchWithRevokedAccessToken(accessToken);
    }

    @Test
    public void testAccessTokenAfterRequestRefreshToken ()
            throws KustvaktException, IOException {
        String code =
                requestAuthorizationCode(confidentialClientId, userAuthHeader);
        JsonNode node = requestTokenWithAuthorizationCodeAndHeader(
                confidentialClientId, code, clientAuthHeader);

        String accessToken = node.at("/access_token").asText();
        String refreshToken = node.at("/refresh_token").asText();

        Form form = new Form();
        form.param("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.param("client_id", confidentialClientId);
        form.param("client_secret", "secret");
        form.param("refresh_token", refreshToken);

        Response response = target().path(API_VERSION).path("oauth2").path("token")
                .request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));

        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertTrue(!refreshToken.equals(node.at("/refresh_token").asText()));

        testSearchWithRevokedAccessToken(accessToken);
    }

    @Test
    public void testRequestAuthorizationWithBearerTokenUnauthorized ()
            throws KustvaktException {
        String code = requestAuthorizationCode(confidentialClientId,
                userAuthHeader);
        JsonNode node = requestTokenWithAuthorizationCodeAndHeader(
                confidentialClientId, code, clientAuthHeader);
        String userAuthToken = node.at("/access_token").asText();

        Response response = requestAuthorizationCode("code",
                confidentialClientId, "", "", "", "Bearer " + userAuthToken);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Scope authorize is not authorized",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testRequestAuthorizationWithBearerToken ()
            throws KustvaktException {
        Response response =
                requestTokenWithDoryPassword(superClientId, clientSecret);
        String entity = response.readEntity(String.class);
        
        JsonNode node = JsonUtils.readTree(entity);
        String userAuthToken = node.at("/access_token").asText();
        assertNotNull(userAuthToken);
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());

        String code = requestAuthorizationCode(superClientId,
                "Bearer " + userAuthToken);
        assertNotNull(code);
    }

}
