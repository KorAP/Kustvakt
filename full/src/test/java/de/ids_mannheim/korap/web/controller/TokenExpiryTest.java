package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class TokenExpiryTest extends SpringJerseyTest {

    @Test
    public void requestToken ()
            throws KustvaktException, InterruptedException, IOException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");
        form.add("client_secret", "secret");
        form.add("username", "dory");
        form.add("password", "password");

        ClientResponse response = resource().path("oauth2").path("token")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        String token = node.at("/access_token").asText();

        Thread.sleep(1000);

        testRequestAuthorizationCodeAuthenticationTooOld(token);
        
        Thread.sleep(1500);
        testSearchWithExpiredToken(token);
    }

    // not possible to store expired token in the test database,
    // because sqlite needs a trigger after INSERT to
    // oauth_access_token to store created_date. Before INSERT trigger
    // does not work.
    private void testSearchWithExpiredToken (String token)
            throws KustvaktException, IOException {
        ClientResponse response = resource().path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION, "Bearer " + token)
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);

        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.EXPIRED, node.at("/errors/0/0").asInt());
        assertEquals("Access token is expired",
                node.at("/errors/0/1").asText());
    }

    // cannot be tested dynamically
    private void testRequestAuthorizationCodeAuthenticationTooOld (String token)
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");
        form.add("redirect_uri",
                "https://korap.ids-mannheim.de/confidential/redirect");
        form.add("scope", "openid");
        form.add("max_age", "1");

        ClientResponse response =
                resource().path("oauth2").path("openid").path("authorize")
                        .header(Attributes.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .entity(form).post(ClientResponse.class);

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.USER_REAUTHENTICATION_REQUIRED,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "User reauthentication is required because the authentication "
                        + "time is too old according to max_age",
                node.at("/errors/0/1").asText());
    }
}
