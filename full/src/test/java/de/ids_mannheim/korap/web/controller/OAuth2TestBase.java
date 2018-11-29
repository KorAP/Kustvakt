package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
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
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * Provides common methods and variables for OAuth2 tests,
 * and does not run any test.
 * 
 * @author margaretha
 *
 */
public abstract class OAuth2TestBase extends SpringJerseyTest {

    protected String publicClientId = "8bIDtZnH6NvRkW2Fq";
    protected String confidentialClientId = "9aHsGW6QflV13ixNpez";
    protected String superClientId = "fCBbQkAyYzI4NzUxMg";
    protected String clientSecret = "secret";

    public ClientResponse requestAuthorizationCode (
            MultivaluedMap<String, String> form, String authHeader)
            throws KustvaktException {

        return resource().path(API_VERSION).path("oauth2").path("authorize")
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    public String requestAuthorizationCode (String clientId,
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

    public ClientResponse requestToken (MultivaluedMap<String, String> form)
            throws KustvaktException {
        return resource().path(API_VERSION).path("oauth2").path("token")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);
    }

    // client credentials as form params
    public ClientResponse requestTokenWithAuthorizationCodeAndForm (
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
    public JsonNode requestTokenWithAuthorizationCodeAndHeader (String clientId,
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

    public ClientResponse requestTokenWithDoryPassword (String clientId,
            String clientSecret) throws KustvaktException {
        return requestTokenWithPassword(clientId, clientSecret, "dory",
                "password");
    }

    public ClientResponse requestTokenWithPassword (String clientId,
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

    public void updateClientPrivilege (MultivaluedMap<String, String> form)
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

    public ClientResponse searchWithAccessToken (String accessToken) {
        return resource().path(API_VERSION).path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
    }

}
