package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;

public class OAuth2OpenIdControllerTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;

    @Test
    public void testAuthorize () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        String redirectUri =
                "https://korap.ids-mannheim.de/confidential/redirect";
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("scope", "openid");
        form.add("redirect_uri", redirectUri);
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");

        ClientResponse response = resource().path("oauth2").path("openid")
                .path("authorize")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        URI location = response.getLocation();

        assertEquals(redirectUri, location.getScheme() + "://"
                + location.getHost() + location.getPath());
        assertTrue(location.getQuery().startsWith("code"));
    }

}
