package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.junit.Test;
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
import de.ids_mannheim.korap.utils.JsonUtils;

public class OAuth2OpenIdControllerTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;

    private String redirectUri =
            "https://korap.ids-mannheim.de/confidential/redirect";

    private ClientResponse sendAuthorizationRequest (
            MultivaluedMap<String, String> form) throws KustvaktException {
        return resource().path("oauth2").path("openid").path("authorize")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    @Test
    public void testRequestAuthorizationCode ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");

        testRequestAuthorizationCodeWithoutOpenID(form, redirectUri);
        form.add("scope", "openid");

        testRequestAuthorizationCodeMissingRedirectUri(form);
        testRequestAuthorizationCodeInvalidRedirectUri(form);
        form.add("redirect_uri", redirectUri);

        form.add("state", "thisIsMyState");

        ClientResponse response = sendAuthorizationRequest(form);
        URI location = response.getLocation();
        assertEquals(redirectUri, location.getScheme() + "://"
                + location.getHost() + location.getPath());

        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUri(location).build().getQueryParams();
        assertNotNull(params.getFirst("code"));
        assertEquals("thisIsMyState", params.getFirst("state"));
    }

    private void testRequestAuthorizationCodeWithoutOpenID (
            MultivaluedMap<String, String> form, String redirectUri)
            throws KustvaktException {
        ClientResponse response = sendAuthorizationRequest(form);
        URI location = response.getLocation();
        // System.out.println(location.toString());
        assertEquals(redirectUri, location.getScheme() + "://"
                + location.getHost() + location.getPath());
    }

    private void testRequestAuthorizationCodeMissingRedirectUri (
            MultivaluedMap<String, String> form) throws KustvaktException {
        ClientResponse response = sendAuthorizationRequest(form);
        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("redirect_uri is required",
                node.at("/error_description").asText());
    }

    private void testRequestAuthorizationCodeInvalidRedirectUri (
            MultivaluedMap<String, String> form) throws KustvaktException {
        form.add("redirect_uri", "blah");
        ClientResponse response = sendAuthorizationRequest(form);
        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid redirect URI",
                node.at("/error_description").asText());

        form.remove("redirect_uri");
    }

    @Test
    public void testRequestAuthorizationCodeMissingClientID ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("scope", "openid");
        form.add("redirect_uri", redirectUri);

        // error response is represented in JSON because redirect URI
        // cannot be verified without client id
        // Besides client_id is a mandatory parameter in a normal
        // OAuth2 authorization request, thus it is checked first,
        // before redirect_uri. see
        // com.nimbusds.oauth2.sdk.AuthorizationRequest

        ClientResponse response = sendAuthorizationRequest(form);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid request: Missing \"client_id\" parameter",
                node.at("/error_description").asText());

    }

    @Test
    public void testRequestAuthorizationCodeMissingResponseType ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("scope", "openid");
        form.add("redirect_uri", redirectUri);
        form.add("client_id", "blah");

        // client_id has not been verified yet
        // MUST NOT automatically redirect the user-agent to the
        // invalid redirection URI.

        ClientResponse response = sendAuthorizationRequest(form);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Invalid request: Missing \"response_type\" parameter",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestAuthorizationCodeUnsupportedResponseType ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("scope", "openid");
        form.add("redirect_uri", redirectUri);
        // we don't support implicit grant
        form.add("response_type", "id_token token");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");
        form.add("nonce", "nonce");

        ClientResponse response = sendAuthorizationRequest(form);
        URI location = response.getLocation();
        // System.out.println(location);

        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUri(location).build().getQueryParams();
        assertEquals("invalid_request", params.getFirst("error"));
        assertEquals("unsupported+response_type%3A+id_token",
                params.getFirst("error_description"));
    }
}
