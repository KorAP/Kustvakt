package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.junit.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Scope;
import de.ids_mannheim.korap.utils.JsonUtils;

public class OAuth2AccessTokenTest extends SpringJerseyTest {

    // normal client
    private String clientId = "9aHsGW6QflV13ixNpez";
    private String superClientId = "fCBbQkAyYzI4NzUxMg";
    private String clientSecret = "secret";

    private String requestAuthorizationCode (String scope, String authHeader)
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        if (scope != null) {
            form.add("scope", scope);
        }

        ClientResponse response = resource().path("oauth2").path("authorize")
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());
        URI redirectUri = response.getLocation();
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(redirectUri).build().getQueryParams();
        return params.getFirst("code");
    }

    // client credentials as form params
    private JsonNode requestTokenWithAuthorizationCodeGrant ()
            throws KustvaktException {
        String authHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
        String code = requestAuthorizationCode(null, authHeader);

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);

        ClientResponse response = resource().path("oauth2").path("token")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    // client credentials in authorization header
    private JsonNode requestTokenWithAuthorizationHeader (String code)
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("code", code);

        ClientResponse response = resource().path("oauth2").path("token")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(clientId,
                                        clientSecret))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    @Test
    public void testScopeWithSuperClient () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("client_id", superClientId);
        form.add("client_secret", clientSecret);
        form.add("username", "dory");
        form.add("password", "password");

        ClientResponse response = resource().path("oauth2").path("token")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals("all", node.at("/scope").asText());
        String accessToken = node.at("/access_token").asText();

        // test list user group
        response = resource().path("group").path("list")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(2, node.size());
    }

    @Test
    public void testCustomAuthorizationScope () throws KustvaktException {
        String authHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
        String code = requestAuthorizationCode(OAuth2Scope.VC_INFO.toString(),
                authHeader);
        JsonNode node = requestTokenWithAuthorizationHeader(code);

        String token = node.at("/access_token").asText();
        assertTrue(node.at("/scope").asText()
                .contains(OAuth2Scope.VC_INFO.toString()));

        ClientResponse response = resource().path("vc").path("list")
                .header(Attributes.AUTHORIZATION, "Bearer " + token)
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(4, node.size());
    }

    @Test
    public void testDefaultScope () throws KustvaktException, IOException {
        String accessToken = requestTokenWithAuthorizationCodeGrant()
                .at("/access_token").asText();
        testListVCScopeNotAuthorized(accessToken);
        testListVCAccessBearerNotAuthorize(accessToken);
        testSearchWithOAuth2Token(accessToken);
    }

    private void testListVCScopeNotAuthorized (String accessToken)
            throws KustvaktException {
        ClientResponse response = resource().path("vc").path("list")
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

    private void testListVCAccessBearerNotAuthorize (String accessToken)
            throws KustvaktException {
        ClientResponse response =
                resource().path("vc").path("access").path("list")
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
        ClientResponse response = resource().path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(25, node.at("/matches").size());
    }

    @Test
    public void testSearchWithUnknownToken ()
            throws KustvaktException, IOException {
        ClientResponse response = resource().path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        "Bearer ljsa8tKNRSczJhk20öhq92zG8z350")
                .get(ClientResponse.class);

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
        String accessToken = requestTokenWithAuthorizationCodeGrant()
                .at("/access_token").asText();
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("token", accessToken);
        form.add("client_id", clientId);
        form.add("client_secret", "secret");

        ClientResponse response = resource().path("oauth2").path("revoke")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testSearchWithRevokedToken(accessToken);
    }

    private void testSearchWithRevokedToken (String accessToken)
            throws KustvaktException {
        ClientResponse response = resource().path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

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
    public void testRevocationAfterRequestRefreshToken ()
            throws KustvaktException {
        JsonNode node = requestTokenWithAuthorizationCodeGrant();
        String accessToken = node.at("/access_token").asText();
        String refreshToken = node.at("/refresh_token").asText();

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.add("client_id", clientId);
        form.add("client_secret", "secret");
        form.add("refresh_token", refreshToken);

        ClientResponse response = resource().path("oauth2").path("token")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertEquals(refreshToken, node.at("/refresh_token").asText());

        testSearchWithRevokedToken(accessToken);
    }

    @Test
    public void testRequestAuthorizationWithBearerTokenUnauthorized () throws KustvaktException {
        String userAuthToken = requestTokenWithAuthorizationCodeGrant()
                .at("/access_token").asText();

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        ClientResponse response = resource().path("oauth2").path("authorize")
                .header(Attributes.AUTHORIZATION, "Bearer " + userAuthToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Scope authorize is not authorized",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testRequestAuthorizationWithBearerToken ()
            throws KustvaktException {
        String userAuthToken = requestTokenWithPasswordGrant();
        String code = requestAuthorizationCode(null, "Bearer " + userAuthToken);
        assertNotNull(code);
    }

    private String requestTokenWithPasswordGrant () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("client_id", superClientId);
        form.add("client_secret", clientSecret);
        form.add("username", "dory");
        form.add("password", "password");

        ClientResponse response = resource().path("oauth2").path("token")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        String token = node.at("/access_token").asText();
        assertNotNull(token);
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
        return token;
    }
}
