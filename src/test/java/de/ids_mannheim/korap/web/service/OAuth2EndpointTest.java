package de.ids_mannheim.korap.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author hanl
 * @date 23/09/2015
 */
// todo: needs servlet container test server!
public class OAuth2EndpointTest extends FastJerseyTest {

    private static String[] credentials;

    @AfterClass
    public static void close() {
        TestHelper.dropUser();
        BeanConfiguration.closeApplication();
    }

    @BeforeClass
    public static void configure() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
        setPackages("de.ids_mannheim.korap.web.service",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");

        TestHelper.setupUser();
        credentials = TestHelper.getUserCredentials();
    }

//    @Test
    public void testAuthorizeClient() {
        ClientResponse response = resource().path("v0.1").path("oauth2")
                .path("register")
                .queryParam("redirect_url", "korap.ids-mannheim.de/redirect")
                .header("Host", "korap.ids-mannheim.de")
                .post(ClientResponse.class);
        assert response.getStatus() == ClientResponse.Status.OK.getStatusCode();
    }

//    @Test
    public void testRevokeClient() {
        ClientResponse response = resource().path("v0.1").path("oauth2")
                .path("register")
                .queryParam("redirect_url", "korap.ids-mannheim.de/redirect")
                .header("Host", "korap.ids-mannheim.de")
                .post(ClientResponse.class);
        assert response.getStatus() == ClientResponse.Status.OK.getStatusCode();
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));

    }

//    @Test
    public void authenticate() {
        String enc = BasicHttpAuth.encode(credentials[0], credentials[1]);
        ClientResponse response = resource().path("v0.1").path("oauth2")
                .path("register")
                .queryParam("redirect_url", "korap.ids-mannheim.de/redirect")
                .header("Host", "korap.ids-mannheim.de")
                .header(Attributes.AUTHORIZATION, enc)
                .post(ClientResponse.class);
        assert response.getStatus() == ClientResponse.Status.OK.getStatusCode();
        String e = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(e);

        String cl_s = node.path("client_secret").asText();
        String cl_id = node.path("client_id").asText();

        response = resource().path("v0.1").path("oauth2").path("authorize")
                .queryParam("client_id", cl_id)
                .queryParam("client_secret", cl_s)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", "korap.ids-mannheim.de/redirect")
//                .header(Attributes.AUTHORIZATION, enc)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(ClientResponse.class);

        e = response.getEntity(String.class);
        assert response.getStatus() == ClientResponse.Status.OK.getStatusCode();
        node = JsonUtils.readTree(e);

        response = resource().path("v0.1").path("oauth2").path("authorize")
                .queryParam("code", node.path("authorization_code").asText())
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", cl_id)
                .queryParam("client_secret", cl_s).post(ClientResponse.class);

        assert response.getStatus() == ClientResponse.Status.OK.getStatusCode();
    }

}
