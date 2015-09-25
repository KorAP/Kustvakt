package de.ids_mannheim.korap.web.service;

import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 23/09/2015
 */
public class OAuth2EndpointTest extends FastJerseyTest {

    @BeforeClass
    public static void configure() {
        BeanConfiguration.loadClasspathContext();
        addClass(OAuthService.class);
        // todo: change korap user personal data!
        String header = BasicHttpAuth.encode("test", "test1");
    }

    @Test
    public void testAuthorizeClient() {
        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("register")
                .queryParam("redirect_url", "korap.ids-mannheim.de/redirect")
                .header("Host", "korap.ids-mannheim.de")
                .post(ClientResponse.class);
    }

}
