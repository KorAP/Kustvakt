package de.ids_mannheim.korap.web.controller;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
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

/**
 * @author margaretha
 *
 */
public class OAuth2ControllerTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;
    private String username = "OAuth2ControllerTest";

    @Test
    public void testRequestTokenUnsupportedGrant ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
//        form.add("grant_type", "blahblah");
        form.add("grant_type", GrantType.REFRESH_TOKEN.name());
        
        ClientResponse response = resource().path("oauth2").path("token")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        System.out.println(response.getStatus());
        System.out.println(response.getEntity(String.class));
    }

}
