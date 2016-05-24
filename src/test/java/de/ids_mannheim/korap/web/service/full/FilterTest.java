package de.ids_mannheim.korap.web.service.full;

import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.web.service.FastJerseyTest;
import org.eclipse.jetty.server.Response;
import org.junit.BeforeClass;
import org.junit.Test;

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
                                TestHelper.getUserCredentials()[0],
                                TestHelper.getUserCredentials()[1]))
                .get(ClientResponse.class);
        assert resp.getStatus() == Response.SC_OK;
    }


    @Test
    public void testDemoAuth () {
        ClientResponse resp = resource().path(getAPIVersion())
                .path("user/info").get(ClientResponse.class);
        assert resp.getStatus() == Response.SC_OK;
    }


    @Test
    public void testUnauthorizedAuth () {
        ClientResponse resp = resource()
                .path(getAPIVersion())
                .path("user/info")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assert resp.getStatus() == Response.SC_UNAUTHORIZED;
    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
    }
}
