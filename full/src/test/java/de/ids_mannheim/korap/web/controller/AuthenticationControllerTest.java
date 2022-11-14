package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.FastJerseyTest;

/**
 * EM: fix tests. New DB does not save users.
 * @author hanl
 * @date 24/09/2015
 */
@Ignore
public class AuthenticationControllerTest extends FastJerseyTest {

    private static String[] credentials;
    
    @Test
    public void testSessionToken() throws KustvaktException {
        String auth = HttpAuthorizationHandler.createBasicAuthorizationHeaderValue( 
                credentials[0], credentials[1]);
        Response response = target().path("auth")
                .path("sessionToken")
                .request()
                .header(Attributes.AUTHORIZATION, auth)
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String en = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(en);
        assertNotNull(node);

        String token = node.path("token").asText();
        String token_type = node.path("token_type").asText();
        String expiration = node.path("expires").asText();
        DateTime ex = TimeUtils.getTime(expiration);
        assertNotEquals("", token);
        assertNotEquals("", token_type);
        assertFalse(TimeUtils.isExpired(ex.getMillis()));

        response = target().path("user")
                .path("info")
                .request()
                .header(Attributes.AUTHORIZATION, token_type + " "+ token)
                .get();
        en = response.readEntity(String.class);

        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        
        response = target().path("auth")
                .path("logout")
                .request()
                .header(Attributes.AUTHORIZATION, token_type + " "+ token)
                .get();
        
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
    }

    @Test
    public void testSessionTokenExpire() throws KustvaktException {
        String auth = HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(
                credentials[0], credentials[1]);
        Response response = target().path("auth")
                .path("sessionToken")
                .request()
                .header(Attributes.AUTHORIZATION, auth)
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String en = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(en);
        assertNotNull(node);

        String token = node.path("token").asText();
        String token_type = node.path("token_type").asText();
        String expiration = node.path("expires").asText();
        DateTime ex = TimeUtils.getTime(expiration);
        assertNotEquals("", token);
        assertNotEquals("", token_type);

        while (true) {
            if (TimeUtils.isExpired(ex.getMillis()))
                break;
        }
        response = target().path("user")
                .path("info")
                .request()
                .header(Attributes.AUTHORIZATION, token_type + " "+ token)
                .get();
        en = response.readEntity(String.class);
        node = JsonUtils.readTree(en);
        assertNotNull(node);

        assertEquals(StatusCodes.BAD_CREDENTIALS, node.at("/errors/0/0").asInt());
        assertEquals(Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
    }


//    @Test
//    public void testBlockingFilterFail() {
//
//    }
//
//
//    @Test
//    public void testBasicLogout () {
//
//    }
//
//
//    @Test
//    public void testSessionTokenLogin () {
//
//    }
//
//
//    @Test
//    public void testSessionTokenLogout () {
//
//    }
//
//
//    @Test
//    public void testOpenIDLogin () {
//
//    }
//
//
//    @Test
//    public void testOpenIDLogout () {
//
//    }
//
//
//    // -- are these even right? auth - authorization
//    @Test
//    public void testOAuth2Login () {
//
//    }
//
//
//    @Test
//    public void testOAuth2Logout () {
//
//    }

    //todo: test basicauth via secure connection

}
