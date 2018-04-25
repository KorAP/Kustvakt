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
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 *
 */
public class OAuth2ControllerTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;

    private ClientResponse requestAuthorizationConfidentialClient (
            MultivaluedMap<String, String> form) throws KustvaktException {

        return resource().path("oauth2").path("authorize")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "fCBbQkAyYzI4NzUxMg", "secret"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    @Test
    public void testAuthorizeConfidentialClient () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");
        form.add("username", "dory");
        form.add("password", "password");

        ClientResponse response = requestAuthorizationConfidentialClient(form);

        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());
        URI redirectUri = response.getLocation();
        assertTrue(redirectUri.getQuery().startsWith("code"));
    }

    @Test
    public void testAuthorizeInvalidRedirectUri () throws KustvaktException {
        String redirectUri = "https://different.uri/redirect";

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");
        form.add("username", "dory");
        form.add("password", "password");
        form.add("redirect_uri", redirectUri);
        ClientResponse response = requestAuthorizationConfidentialClient(form);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals(redirectUri + " is unknown",
                node.at("/error_description").asText());
    }

    @Test
    public void testAuthorizeMissingRequiredParameters ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        // missing code
        ClientResponse response = requestAuthorizationConfidentialClient(form);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing response_type parameter value",
                node.at("/error_description").asText());

        // missing client_id
        form.add("response_type", "code");
        response = requestAuthorizationConfidentialClient(form);
        entity = response.getEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals("Missing parameters: client_id",
                node.at("/error_description").asText());
    }

    @Test
    public void testAuthorizeInvalidResponseType () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "string");

        ClientResponse response = requestAuthorizationConfidentialClient(form);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Invalid response_type parameter value",
                node.at("/error_description").asText());
    }

    private ClientResponse requestToken (MultivaluedMap<String, String> form)
            throws KustvaktException {
        return resource().path("oauth2").path("token")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    @Test
    public void testRequestTokenAuthorizationConfidential ()
            throws KustvaktException {
        
        MultivaluedMap<String, String> authForm = new MultivaluedMapImpl();
        authForm.add("response_type", "code");
        authForm.add("client_id", "fCBbQkAyYzI4NzUxMg");
        authForm.add("username", "dory");
        authForm.add("password", "password");
//        form.add("scope", "read");
        ClientResponse response = requestAuthorizationConfidentialClient(authForm);
        URI redirectUri = response.getLocation();
        String code = redirectUri.getQuery().split("=")[1];
        
        MultivaluedMap<String, String> tokenForm = new MultivaluedMapImpl();
        tokenForm.add("grant_type", "authorization_code");
        tokenForm.add("client_id", "fCBbQkAyYzI4NzUxMg");
        tokenForm.add("client_secret", "secret");
        tokenForm.add("code", code);
        
        response = requestToken(tokenForm);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }
    
    @Test
    public void testRequestTokenPasswordGrantConfidential ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");
        form.add("client_secret", "secret");
        form.add("username", "dory");
        form.add("password", "password");

        ClientResponse response = requestToken(form);
        String entity = response.getEntity(String.class);

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

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("username", "dory");
        form.add("password", "password");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");

        ClientResponse response = requestToken(form);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing parameters: client_secret",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantMissingClientId ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("username", "dory");
        form.add("password", "password");
        form.add("client_secret", "secret");

        ClientResponse response = requestToken(form);
        String entity = response.getEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing parameters: client_id",
                node.at("/error_description").asText());
    }
    
    @Test
    public void testRequestTokenPasswordGrantPublic ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("username", "dory");
        form.add("password", "password");
        form.add("client_id", "iBr3LsTCxOj7D2o0A5m");

        ClientResponse response = requestToken(form);
        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantNonNative ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("username", "dory");
        form.add("password", "password");
        // confidential nonnative
        form.add("client_id", "9aHsGW6QflV13ixNpez");
        form.add("client_secret", "secret");

        ClientResponse response = requestToken(form);
        String entity = response.getEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT,
                node.at("/error").asText());
        assertEquals("Password grant is not allowed for third party clients",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenClientCredentialsGrant ()
            throws KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "client_credentials");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");
        form.add("client_secret", "secret");
        ClientResponse response = requestToken(form);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        // length?
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }

    @Test
    public void testRequestTokenMissingGrantType () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        ClientResponse response = requestToken(form);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
    }

    @Test
    public void testRequestTokenUnsupportedGrant () throws KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "blahblah");

        ClientResponse response = resource().path("oauth2").path("token")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("Invalid grant_type parameter value",
                node.get("error_description").asText());
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.get("error").asText());
    }

}
