package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * Before running this test:
 * set oauth2.access.token.expiry = 2S
 * oauth2.authorization.code.expiry = 1S
 * 
 * @author margaretha
 *
 */
public class TokenExpiryTest extends SpringJerseyTest {

    @Test
    public void requestToken ()
            throws KustvaktException, InterruptedException, IOException {
        Form form = new Form();
        form.param("grant_type", "password");
        form.param("client_id", "fCBbQkAyYzI4NzUxMg");
        form.param("client_secret", "secret");
        form.param("username", "dory");
        form.param("password", "password");

        Response response = target().path(API_VERSION).path("oauth2").path("token")
                .request()
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));

        String entity = response.readEntity(String.class);
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
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .request()
                .header(Attributes.AUTHORIZATION, "Bearer " + token)
                .get();

        String ent = response.readEntity(String.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.EXPIRED, node.at("/errors/0/0").asInt());
        assertEquals("Access token is expired",
                node.at("/errors/0/1").asText());
    }

    // cannot be tested dynamically
    private void testRequestAuthorizationCodeAuthenticationTooOld (String token)
            throws KustvaktException {
        Form form = new Form();
        form.param("response_type", "code");
        form.param("client_id", "fCBbQkAyYzI4NzUxMg");
        form.param("redirect_uri",
                "https://korap.ids-mannheim.de/confidential/redirect");
        form.param("scope", "openid");
        form.param("max_age", "1");

        Response response =
                target().path(API_VERSION).path("oauth2").path("openid").path("authorize")
                        .request()
                        .header(Attributes.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .post(Entity.form(form));

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.USER_REAUTHENTICATION_REQUIRED,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "User reauthentication is required because the authentication "
                        + "time is too old according to max_age",
                node.at("/errors/0/1").asText());
    }
}
