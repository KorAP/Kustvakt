package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.OAuth2ClientType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

public class OAuth2ClientControllerTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;
    private String username = "OAuth2ClientControllerTest";
    
    @Test
    public void testRegisterClient () throws KustvaktException {

        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2ClientTest");
        json.setType(OAuth2ClientType.CONFIDENTIAL);
        json.setUrl("http://example.client.com");
        json.setRedirectURI("http://example.client.com/redirect");

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
        assertNotNull(node.at("/client_id").asText());
        assertNotNull(node.at("/client_secret").asText());
    }
}
