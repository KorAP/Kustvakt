package de.ids_mannheim.korap.web.controller.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class OAuth2DeprecationTest extends OAuth2TestBase{
	private String userAuthHeader;
	private String username = "dory";

    public OAuth2DeprecationTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, "password");
    }

	@Test
	public void testListClients () throws KustvaktException {
		// authorized client
		String code = requestAuthorizationCode(publicClientId, userAuthHeader);
		Response response = requestTokenWithAuthorizationCodeAndForm(
				publicClientId, "", code);
		
		// owned client
		OAuth2ClientJson clientJson = createOAuth2ClientJson(
                "OAuth2DesktopClient", OAuth2ClientType.PUBLIC,
                "This is a desktop test client.");
        response = registerClient(username, clientJson);
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());

        testListAuthorizedClients(publicClientId);
        testListOwnedClient(clientId);
        testFilterBy(publicClientId);
        
        response = target().path(API_VERSION).path("oauth2")
                .path("client").path("deregister").path(clientId).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}
	
	private void testListAuthorizedClients(String clientId) throws KustvaktException {
		// List clients
		Form form = getSuperClientForm();
		form.param("authorized_only","true");
		// V1.0
        Response response = target().path(API_VERSION_V1_0).path("oauth2")
                .path("client").path("list").request()
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
        assertEquals(clientId, node.at("/0/client_id").asText());
        
        // current version
        response = target().path(API_VERSION).path("oauth2")
                .path("client").path("list").request()
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
	}
	
	private void testListOwnedClient (String clientId) throws KustvaktException {
		// List clients
		Form form = getSuperClientForm();
		// V1.0
        Response response = target().path(API_VERSION_V1_0).path("oauth2")
                .path("client").path("list").request()
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
        assertEquals(clientId, node.at("/0/client_id").asText());
	}
	
	private void testFilterBy (String clientId) throws KustvaktException {
		// List clients
		Form form = getSuperClientForm();
		form.param("filter_by","authorized_only");
		// V1.0
        Response response = target().path(API_VERSION_V1_0).path("oauth2")
                .path("client").path("list").request()
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
        assertEquals(clientId, node.at("/0/client_id").asText());
	}
	
}
