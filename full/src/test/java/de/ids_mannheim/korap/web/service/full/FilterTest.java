package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;

import org.eclipse.jetty.server.Response;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.TokenType;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/** EM: fix tests. new DB does not save users.
 * @author hanl
 * @date 08/02/2016
 */
@Ignore
public class FilterTest extends FastJerseyTest {

    @Autowired
    HttpAuthorizationHandler handler;


    @Test
    public void testTestUserAuth () throws UniformInterfaceException, ClientHandlerException, 
        KustvaktException {
        
        ClientResponse resp = resource()
                
                .path("user/info")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                (String) TestHelper.getUserCredentials().get(Attributes.USERNAME),
                                (String) TestHelper.getUserCredentials().get(Attributes.PASSWORD)))
                .get(ClientResponse.class);
        assert resp.getStatus() == Response.SC_OK;
    }


    @Test
    @Ignore
    public void testDemoAuth () {
        ClientResponse resp = resource()
                .path("user/info").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(), resp.getStatus());
    }


    @Test
    public void testUnauthorizedAuth () throws UniformInterfaceException, 
        ClientHandlerException, KustvaktException {
        
        ClientResponse resp = resource()
                .path("user/info")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        String entity = resp.getEntity(String.class);
        System.out.println(entity);
        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(), resp.getStatus());
    }


    @Override
    public void initMethod () throws KustvaktException {
//        helper().setupAccount();
    }
}
