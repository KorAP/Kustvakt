package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ContainerRequest;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

/**
 * @author margaretha
 *
 */
public class OAuth2ClientControllerTest extends SpringJerseyTest {

    private String username = "OAuth2ClientControllerTest";

    private void checkWWWAuthenticateHeader (ClientResponse response) {
        Set<Entry<String, List<String>>> headers =
                response.getHeaders().entrySet();

        for (Entry<String, List<String>> header : headers) {
            if (header.getKey().equals(ContainerRequest.WWW_AUTHENTICATE)) {
                assertEquals("Basic realm=\"Kustvakt\"",
                        header.getValue().get(0));
            }
        }
    }

    private ClientResponse registerConfidentialClient ()
            throws KustvaktException {

        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2ClientTest");
        json.setType(OAuth2ClientType.CONFIDENTIAL);
        json.setUrl("http://example.client.com");
        json.setRedirectURI("https://example.client.com/redirect");
        json.setDescription("This is a confidential test client.");

        return resource().path("oauth2").path("client").path("register")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);
    }

    @Test
    public void testRegisterConfidentialClient () throws KustvaktException {
        ClientResponse response = registerConfidentialClient();
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();
        assertNotNull(clientId);
        assertNotNull(clientSecret);

        testRegisterClientNonUniqueURL();

        String newclientSecret =
                testResetConfidentialClientSecret(clientId, clientSecret);

        testDeregisterConfidentialClientMissingSecret(clientId);
        testDeregisterClientIncorrectCredentials(clientId, clientSecret);
        testDeregisterConfidentialClient(clientId, newclientSecret);
    }


    private void testRegisterClientNonUniqueURL () throws KustvaktException {
        ClientResponse response = registerConfidentialClient();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
    }

    @Test
    public void testRegisterPublicClient () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2PublicClient");
        json.setType(OAuth2ClientType.PUBLIC);
        json.setUrl("http://test.public.client.com");
        json.setRedirectURI("https://test.public.client.com/redirect");
        json.setDescription("This is a public test client.");

        ClientResponse response = resource().path("oauth2").path("client")
                .path("register")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());

        testDeregisterPublicClientMissingUserAuthentication(clientId);
        testDeregisterPublicClientMissingId();
        testDeregisterPublicClient(clientId);
    }

    @Test
    public void testRegisterDesktopApp () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2DesktopClient");
        json.setType(OAuth2ClientType.PUBLIC);
        json.setDescription("This is a desktop test client.");

        ClientResponse response = resource().path("oauth2").path("client")
                .path("register")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());

        testResetPublicClientSecret(clientId);
    }

    @Test
    public void testRegisterNativeClient () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("NativeClient");
        json.setType(OAuth2ClientType.PUBLIC);
        json.setUrl("http://korap.ids-mannheim.de/native");
        json.setRedirectURI("https://korap.ids-mannheim.de/native/redirect");
        json.setDescription("This is a native test client.");

        ClientResponse response = resource().path("oauth2").path("client")
                .path("register")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();

        // EM: need to check native

        testAccessTokenAfterDeregistration(clientId);
    }

    private void testAccessTokenAfterDeregistration (String clientId)
            throws KustvaktException {
        String code = requestAuthorizationCode(clientId, "");
        ClientResponse response = requestAccessToken(code, clientId, "");
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken = node.at("/access_token").asText();

        response = searchWithAccessToken(accessToken);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        code = requestAuthorizationCode(clientId, "");
        testDeregisterPublicClient(clientId);

        response = requestAccessToken(code, clientId, "");
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(OAuth2Error.INVALID_CLIENT.toString(),
                node.at("/error").asText());

        response = searchWithAccessToken(accessToken);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token has been revoked",
                node.at("/errors/0/1").asText());

    }

    private String requestAuthorizationCode (String clientId,
            String clientSecret) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        ClientResponse response = resource().path("oauth2").path("authorize")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("dory",
                                        "password"))
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

    private ClientResponse requestAccessToken (String code, String clientId,
            String clientSecret) throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);

        ClientResponse response = resource().path("oauth2").path("token")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        return response;
    }

    private ClientResponse searchWithAccessToken (String accessToken) {
        ClientResponse response = resource().path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        return response;
    }

    private void testDeregisterPublicClientMissingUserAuthentication (
            String clientId) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        ClientResponse response = resource().path("oauth2").path("client")
                .path("deregister").path(clientId).delete(ClientResponse.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    private void testDeregisterPublicClientMissingId ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        ClientResponse response = resource().path("oauth2").path("client")
                .path("deregister")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    private void testDeregisterPublicClient (String clientId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        ClientResponse response = resource().path("oauth2").path("client")
                .path("deregister").path(clientId)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testDeregisterConfidentialClient (String clientId,
            String clientSecret) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_secret", clientSecret);

        ClientResponse response = resource().path("oauth2").path("client")
                .path("deregister").path(clientId)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testDeregisterConfidentialClientMissingSecret (String clientId)
            throws KustvaktException {

        ClientResponse response = resource().path("oauth2").path("client")
                .path("deregister").path(clientId)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .delete(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Missing parameters: client_secret",
                node.at("/error_description").asText());
    }

    private void testDeregisterClientIncorrectCredentials (String clientId,
            String clientSecret) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_secret", clientSecret);

        ClientResponse response = resource().path("oauth2").path("client")
                .path("deregister").path(clientId)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).delete(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_CLIENT, node.at("/error").asText());
        assertEquals("Invalid client credentials",
                node.at("/error_description").asText());

        checkWWWAuthenticateHeader(response);
    }

    private void testResetPublicClientSecret (String clientId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);

        ClientResponse response = resource().path("oauth2").path("client")
                .path("reset")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Operation is not allowed for public clients",
                node.at("/error_description").asText());
    }

    private String testResetConfidentialClientSecret (String clientId,
            String clientSecret) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        ClientResponse response = resource().path("oauth2").path("client")
                .path("reset")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(clientId, node.at("/client_id").asText());

        String newClientSecret = node.at("/client_secret").asText();
        assertTrue(!clientSecret.equals(newClientSecret));

        return newClientSecret;
    }
}
