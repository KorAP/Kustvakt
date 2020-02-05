package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.RefreshTokenDao;
import de.ids_mannheim.korap.utils.JsonUtils;

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
    protected String confidentialClientId = "9aHsGW6QflV13ixNpez";
    protected String confidentialClientId2 = "9aHsGW6QflV13ixNpez";
    protected String superClientId = "fCBbQkAyYzI4NzUxMg";
    protected String clientSecret = "secret";

    protected ClientResponse requestAuthorizationCode (
            MultivaluedMap<String, String> form, String authHeader)
            throws KustvaktException {

        return resource().path(API_VERSION).path("oauth2").path("authorize")
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    protected String requestAuthorizationCode (String clientId,
            String clientSecret, String scope, String authHeader)
            throws KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        if (scope != null) {
            form.add("scope", scope);
        }

        ClientResponse response = requestAuthorizationCode(form, authHeader);
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

    // client credentials in authorization header
    protected JsonNode requestTokenWithAuthorizationCodeAndHeader (String clientId,
            String code, String authHeader) throws KustvaktException {
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

    protected void updateClientPrivilege (MultivaluedMap<String, String> form)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("privilege")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    protected ClientResponse searchWithAccessToken (String accessToken) {
        return resource().path(API_VERSION).path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
    }
    
    protected void testRevokeTokenViaSuperClient (String token, String userAuthHeader) {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("token", token);
        form.add("super_client_id", superClientId);
        form.add("super_client_secret", clientSecret);

        ClientResponse response = resource().path(API_VERSION)
                .path("oauth2").path("revoke").path("super")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("SUCCESS", response.getEntity(String.class));
    }

}
