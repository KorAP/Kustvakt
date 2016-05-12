package de.ids_mannheim.korap.web.service.full;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author hanl
 * @date 23/09/2015
 */
// todo: in combination with other tests, causes failures!
public class OAuth2EndpointTest extends FastJerseyTest {

    @BeforeClass
    public static void configure() throws Exception {
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }

    @Test
    public void init() {

    }

    @Override
    public void initMethod() throws KustvaktException {
        helper().setupAccount();
    }

    @Test
    @Ignore
    public void testAuthorizeClient() {
        ClientResponse response = resource().path("v0.1").path("oauth2")
                .path("register")
                .queryParam("redirect_url", "korap.ids-mannheim.de/redirect")
                .header("Host", "korap.ids-mannheim.de")
                .post(ClientResponse.class);
        assert response.getStatus() == ClientResponse.Status.OK.getStatusCode();
    }

    @Test
    @Ignore
    public void testRevokeClient() {
        ClientResponse response = resource().path("v0.1").path("oauth2")
                .path("register")
                .queryParam("redirect_url", "korap.ids-mannheim.de/redirect")
                .header("Host", "korap.ids-mannheim.de")
                .post(ClientResponse.class);
        assert response.getStatus() == ClientResponse.Status.OK.getStatusCode();
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));

    }

    @Test
    @Ignore
    public void authenticate() {
        String[] cred = TestHelper.getUserCredentials();
        String enc = BasicHttpAuth.encode(cred[0], cred[1]);
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
