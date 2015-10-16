package de.ids_mannheim.korap.web.service;

import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 23/09/2015
 */
// todo: needs servlet container test server!
public class OAuth2EndpointTest extends FastJerseyTest {

    @AfterClass
    public static void close() {
        BeanConfiguration.closeApplication();
    }

    @BeforeClass
    public static void configure() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
        setPackages("de.ids_mannheim.korap.web.service",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");

        TestHelper.setup();
        String[] cred = TestHelper.getCredentials();

        String header = BasicHttpAuth.encode(cred[0], cred[1]);
    }

    @Test
    public void testAuthorizeClient() {
        ClientResponse response = resource().path("v0.2").path("oauth2")
                .path("register")
                .queryParam("redirect_url", "korap.ids-mannheim.de/redirect")
                .header("Host", "korap.ids-mannheim.de")
                .post(ClientResponse.class);
        System.out.println(response);
    }

    public void testRevokeClient() {

    }

}
