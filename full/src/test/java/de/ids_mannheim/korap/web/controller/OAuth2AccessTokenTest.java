package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

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
        ClientResponse response =
                requestTokenWithDoryPassword(superClientId, clientSecret);

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals("all", node.at("/scope").asText());
        String accessToken = node.at("/access_token").asText();

        // test list user group
        response = resource().path(API_VERSION).path("group").path("list")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(2, node.size());
    }

    @Test
    public void testCustomScope () throws KustvaktException {
        String code = requestAuthorizationCode(confidentialClientId,
                clientSecret, OAuth2Scope.VC_INFO.toString(), userAuthHeader);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));

        String token = node.at("/access_token").asText();
        assertTrue(node.at("/scope").asText()
                .contains(OAuth2Scope.VC_INFO.toString()));

        // test list vc using the token
        response = resource().path(API_VERSION).path("vc").path("list")
                .header(Attributes.AUTHORIZATION, "Bearer " + token)
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(4, node.size());
    }

    @Test
    public void testDefaultScope () throws KustvaktException, IOException {
        String code = requestAuthorizationCode(confidentialClientId, clientSecret,
                null, userAuthHeader);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken = node.at("/access_token").asText();
        testScopeNotAuthorized(accessToken);
        testScopeNotAuthorize2(accessToken);
        testSearchWithOAuth2Token(accessToken);
    }

    private void testScopeNotAuthorized (String accessToken)
            throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("vc").path("list")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Scope vc_info is not authorized",
                node.at("/errors/0/1").asText());
    }

    private void testScopeNotAuthorize2 (String accessToken)
            throws KustvaktException {
        ClientResponse response =
                resource().path(API_VERSION).path("vc").path("access").path("list")
                        .header(Attributes.AUTHORIZATION,
                                "Bearer " + accessToken)
                        .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Scope vc_access_info is not authorized",
                node.at("/errors/0/1").asText());
    }

    private void testSearchWithOAuth2Token (String accessToken)
            throws KustvaktException, IOException {
        ClientResponse response = searchWithAccessToken(accessToken);
        String entity = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals(25, node.at("/matches").size());
    }

    @Test
    public void testSearchWithUnknownToken ()
            throws KustvaktException, IOException {
        ClientResponse response =
                searchWithAccessToken("ljsa8tKNRSczJhk20öhq92zG8z350");

        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is not found",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testRevokeAccessTokenConfidentialClient ()
            throws KustvaktException {
        String code = requestAuthorizationCode(confidentialClientId,
                clientSecret, null, userAuthHeader);
        JsonNode node = requestTokenWithAuthorizationCodeAndHeader(
                confidentialClientId, code, clientAuthHeader);

        String accessToken = node.at("/access_token").asText();
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("token", accessToken);
        form.add("client_id", confidentialClientId);
        form.add("client_secret", "secret");

        ClientResponse response = resource().path(API_VERSION).path("oauth2").path("revoke")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testSearchWithRevokedAccessToken(accessToken);
    }

    private void testSearchWithRevokedAccessToken (String accessToken)
            throws KustvaktException {
        ClientResponse response = searchWithAccessToken(accessToken);
        String entity = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token has been revoked",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testAccessTokenAfterRequestRefreshToken ()
            throws KustvaktException, IOException {
        String code = requestAuthorizationCode(confidentialClientId,
                clientSecret, null, userAuthHeader);
        JsonNode node = requestTokenWithAuthorizationCodeAndHeader(
                confidentialClientId, code, clientAuthHeader);

        String accessToken = node.at("/access_token").asText();
        String refreshToken = node.at("/refresh_token").asText();

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.add("client_id", confidentialClientId);
        form.add("client_secret", "secret");
        form.add("refresh_token", refreshToken);

        ClientResponse response = resource().path(API_VERSION).path("oauth2").path("token")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
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
                clientSecret, null, userAuthHeader);
        JsonNode node = requestTokenWithAuthorizationCodeAndHeader(
                confidentialClientId, code, clientAuthHeader);
        String userAuthToken = node.at("/access_token").asText();

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", confidentialClientId);
        form.add("client_secret", clientSecret);

        ClientResponse response = resource().path(API_VERSION).path("oauth2").path("authorize")
                .header(Attributes.AUTHORIZATION, "Bearer " + userAuthToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Scope authorize is not authorized",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testRequestAuthorizationWithBearerToken ()
            throws KustvaktException {
        ClientResponse response =
                requestTokenWithDoryPassword(superClientId, clientSecret);
        String entity = response.getEntity(String.class);
        
        JsonNode node = JsonUtils.readTree(entity);
        String userAuthToken = node.at("/access_token").asText();
        assertNotNull(userAuthToken);
        assertEquals(TokenType.BEARER.displayName(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());

        String code = requestAuthorizationCode(superClientId,
                clientSecret, null, "Bearer " + userAuthToken);
        assertNotNull(code);
    }

}
