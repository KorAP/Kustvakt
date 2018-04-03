package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.ClientType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

public class OAuth2ClientControllerTest extends SpringJerseyTest {

    @Test
    public void testRegisterClient () throws KustvaktException {

        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("OAuth2ClientControllerTest");
        json.setType(ClientType.CONFIDENTIAL);
        json.setUrl("http://example.client.com");
        json.setRedirectURI("http://example.client.com/redirect");

        ClientResponse response = resource().path("client").path("register")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
}
