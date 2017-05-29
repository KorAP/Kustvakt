package de.ids_mannheim.korap.web.service.full;

import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.web.service.FastJerseyTest;
import org.eclipse.jetty.server.Response;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author hanl
 * @date 08/02/2016
 */
public class FilterTest extends FastJerseyTest {

    @BeforeClass
    public static void setup () throws Exception {
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }


    @Test
    public void testTestUserAuth () {
        ClientResponse resp = resource()
                .path(getAPIVersion())
                .path("user/info")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode(
                                (String) TestHelper.getUserCredentials().get(Attributes.USERNAME),
                                (String) TestHelper.getUserCredentials().get(Attributes.PASSWORD)))
                .get(ClientResponse.class);
        assert resp.getStatus() == Response.SC_OK;
    }


    @Test
    @Ignore
    public void testDemoAuth () {
        ClientResponse resp = resource().path(getAPIVersion())
                .path("user/info").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(), resp.getStatus());
    }


    @Test
    public void testUnauthorizedAuth () {
        ClientResponse resp = resource()
                .path(getAPIVersion())
                .path("user/info")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        String entity = resp.getEntity(String.class);
        System.out.println(entity);
        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(), resp.getStatus());
    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
    }
}
