package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class OAuth2AccessTokenTest extends OAuth2TestBase {

    private String userAuthHeader;

    private String clientAuthHeader;

    public OAuth2AccessTokenTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
        clientAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(confidentialClientId,
                        clientSecret);
    }
    
    @Test
    public void testScopeWithSuperClient () throws KustvaktException {
        Response response = requestTokenWithDoryPassword(superClientId,
                clientSecret);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(node.at("/scope").asText(), "all");
        String accessToken = node.at("/access_token").asText();
        // test list user group
        response = target().path(API_VERSION).path("group").request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(2, node.size());
    }

    @Test
    public void testCustomScope () throws KustvaktException {
        Response response = requestAuthorizationCode("code",
                confidentialClientId, "", OAuth2Scope.VC_INFO.toString(), "",
                userAuthHeader);
        String code = parseAuthorizationCode(response);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String token = node.at("/access_token").asText();
        String refreshToken = node.at("/refresh_token").asText();
        
        assertTrue(node.at("/scope").asText()
                .contains(OAuth2Scope.VC_INFO.toString()));
        // test list vc using the token
        response = target().path(API_VERSION).path("vc").request()
                .header(Attributes.AUTHORIZATION, "Bearer " + token).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(4, node.size());
        
        revokeToken(token, confidentialClientId, clientSecret,
                ACCESS_TOKEN_TYPE);
        revokeToken(refreshToken, confidentialClientId, clientSecret,
                REFRESH_TOKEN_TYPE);
    }

    @Test
    public void testDefaultScope () throws KustvaktException, IOException {
        String code = requestAuthorizationCode(confidentialClientId,
                userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken = node.at("/access_token").asText();
        String refreshToken = node.at("/refresh_token").asText();
        testScopeNotAuthorized(accessToken);
        testScopeNotAuthorize2(accessToken);
        testSearchWithOAuth2Token(accessToken);
        
        revokeToken(accessToken, confidentialClientId, clientSecret,
                ACCESS_TOKEN_TYPE);
        revokeToken(refreshToken, confidentialClientId, clientSecret,
                REFRESH_TOKEN_TYPE);
    }

    private void testScopeNotAuthorized (String accessToken)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc").request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .get();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Scope vc_info is not authorized");
    }

    private void testScopeNotAuthorize2 (String accessToken)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("access")
                .request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Scope vc_access_info is not authorized");
    }

    @Test
    public void testSearchWithUnknownToken ()
            throws KustvaktException, IOException {
        Response response = searchWithAccessToken(
                "ljsa8tKNRSczJhk20öhq92zG8z350");
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Access token is invalid");
    }

    @Test
    public void testRevokeAccessTokenConfidentialClient ()
            throws KustvaktException {
        String code = requestAuthorizationCode(confidentialClientId,
                userAuthHeader);
        JsonNode node = requestTokenWithAuthorizationCodeAndHeader(
                confidentialClientId, code, clientAuthHeader);
        String accessToken = node.at("/access_token").asText();
        String refreshToken = node.at("/refresh_token").asText();
        
        revokeToken(accessToken, confidentialClientId, clientSecret,
                ACCESS_TOKEN_TYPE);
        testSearchWithRevokedAccessToken(accessToken);
        
        revokeToken(refreshToken, confidentialClientId, clientSecret,
                REFRESH_TOKEN_TYPE);
    }

    @Test
    public void testRevokeAccessTokenPublicClientViaSuperClient ()
            throws KustvaktException {
        String code = requestAuthorizationCode(publicClientId, userAuthHeader);
        Response response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, "", code);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken = node.at("/access_token").asText();
        revokeTokenViaSuperClient(accessToken, userAuthHeader);
        testSearchWithRevokedAccessToken(accessToken);
    }

    @Test
    public void testAccessTokenAfterRequestRefreshToken ()
            throws KustvaktException, IOException {
        String code = requestAuthorizationCode(confidentialClientId,
                userAuthHeader);
        JsonNode node = requestTokenWithAuthorizationCodeAndHeader(
                confidentialClientId, code, clientAuthHeader);
        String accessToken = node.at("/access_token").asText();
        String refreshToken = node.at("/refresh_token").asText();

        node = requestTokenWithRefreshToken(confidentialClientId, clientSecret,
                refreshToken);
        String newAccessToken = node.at("/access_token").asText();
        String newRefreshToken = node.at("/refresh_token").asText();
        assertTrue(!refreshToken.equals(newRefreshToken));

        testSearchWithRevokedAccessToken(accessToken);
        revokeToken(newAccessToken, confidentialClientId, clientSecret,
                ACCESS_TOKEN_TYPE);
        revokeToken(newRefreshToken, confidentialClientId, clientSecret,
                REFRESH_TOKEN_TYPE);
    }

    @Test
    public void testRequestAuthorizationWithBearerTokenUnauthorized ()
            throws KustvaktException {
        String code = requestAuthorizationCode(confidentialClientId,
                userAuthHeader);
        JsonNode node = requestTokenWithAuthorizationCodeAndHeader(
                confidentialClientId, code, clientAuthHeader);
        String userAuthToken = node.at("/access_token").asText();
        String refreshToken = node.at("/refresh_token").asText();
        Response response = requestAuthorizationCode("code",
                confidentialClientId, "", "search", "",
                "Bearer " + userAuthToken);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Scope authorize is not authorized");

        revokeToken(userAuthToken, confidentialClientId, clientSecret,
                ACCESS_TOKEN_TYPE);
        revokeToken(refreshToken, confidentialClientId, clientSecret,
                REFRESH_TOKEN_TYPE);
    }

    @Test
    public void testRequestAuthorizationWithBearerToken ()
            throws KustvaktException {
        Response response = requestTokenWithDoryPassword(superClientId,
                clientSecret);
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
        
        revokeTokenViaSuperClient(userAuthToken, userAuthHeader);
    }
}
