package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
import de.ids_mannheim.korap.constant.OAuth2ClientType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

/**
 * @author margaretha
 *
 */
public class OAuth2ClientControllerTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;
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

    private ClientResponse testRegisterConfidentialClient ()
            throws KustvaktException {

        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2ClientTest");
        json.setType(OAuth2ClientType.CONFIDENTIAL);
        json.setUrl("http://example.client.com");
        json.setRedirectURI("https://example.client.com/redirect");

        return resource().path("oauth2").path("client").path("register")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);
    }

    @Test
    public void testRegisterClientNonUniqueURL () throws KustvaktException {
        ClientResponse response = testRegisterConfidentialClient();
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();
        assertNotNull(clientId);
        assertNotNull(clientSecret);

        response = testRegisterConfidentialClient();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());

        testDeregisterClientIncorrectCredentials(clientId);
        testDeregisterConfidentialClient(clientId, clientSecret);
    }

    @Test
    public void testRegisterPublicClient () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2PublicClient");
        json.setType(OAuth2ClientType.PUBLIC);
        json.setUrl("http://test.public.client.com");
        json.setRedirectURI("https://test.public.client.com/redirect");

        ClientResponse response = resource().path("oauth2").path("client")
                .path("register")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());

        testDeregisterPublicClient(clientId);
    }

    @Test
    public void testRegisterNativeClient () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("NativeClient");
        json.setType(OAuth2ClientType.PUBLIC);
        json.setUrl("http://korap.ids-mannheim.de/native");
        json.setRedirectURI("https://korap.ids-mannheim.de/native/redirect");

        ClientResponse response = resource().path("oauth2").path("client")
                .path("register")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        //EM: need to check native
    }

    private void testDeregisterPublicClient (String clientId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);

        ClientResponse response = resource().path("oauth2").path("client")
                .path("deregister").path("public")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testDeregisterConfidentialClient (String clientId,
            String clientSecret) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);

        ClientResponse response = resource().path("oauth2").path("client")
                .path("deregister").path("confidential")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(clientId,
                                clientSecret))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testDeregisterClientIncorrectCredentials (String clientId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);

        ClientResponse response = resource().path("oauth2").path("client")
                .path("deregister").path("confidential")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(clientId,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).delete(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_CLIENT,
                node.at("/error").asText());
        assertEquals("Invalid client credentials.",
                node.at("/error_description").asText());

        checkWWWAuthenticateHeader(response);
    }
}
