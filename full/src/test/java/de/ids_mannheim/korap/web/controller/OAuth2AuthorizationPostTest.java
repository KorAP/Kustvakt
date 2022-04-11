package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.junit.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class OAuth2AuthorizationPostTest extends OAuth2TestBase {

    public String userAuthHeader;

    public OAuth2AuthorizationPostTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
    }
    
    private ClientResponse requestAuthorizationCode (
            MultivaluedMap<String, String> form, String authHeader)
            throws KustvaktException {

        return resource().path(API_VERSION).path("oauth2").path("authorize")
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }
    
    @Test
    public void testAuthorizeConfidentialClient () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", confidentialClientId);
        form.add("state", "thisIsMyState");

        ClientResponse response =
                requestAuthorizationCode(form, userAuthHeader);

        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());
        URI redirectUri = response.getLocation();
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(redirectUri).build().getQueryParams();
        assertNotNull(params.getFirst("code"));
        assertEquals("thisIsMyState", params.getFirst("state"));
    }
    
    @Test
    public void testRequestTokenAuthorizationConfidential ()
            throws KustvaktException {

        MultivaluedMap<String, String> authForm = new MultivaluedMapImpl();
        authForm.add("response_type", "code");
        authForm.add("client_id", confidentialClientId);
        authForm.add("scope", "search");

        ClientResponse response =
                requestAuthorizationCode(authForm, userAuthHeader);
        URI redirectUri = response.getLocation();
        MultivaluedMap<String, String> params =
                UriComponent.decodeQuery(redirectUri, true);
        String code = params.get("code").get(0);
        String scopes = params.get("scope").get(0);

        assertEquals(scopes, "search");

        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }
}
