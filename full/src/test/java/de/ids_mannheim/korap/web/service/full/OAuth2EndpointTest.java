package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

import de.ids_mannheim.korap.authentication.BasicHttpAuth;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/**
 * @author hanl
 * @date 23/09/2015
 */
@Ignore
// todo: in combination with other tests, causes failures!
public class OAuth2EndpointTest extends FastJerseyTest {

    @Override
    public void initMethod () throws KustvaktException {
//        helper().setupAccount();
    }


    @Test
    public void testAuthorizeClient () throws ClientHandlerException, UniformInterfaceException, KustvaktException {
        String auth = BasicHttpAuth.encode(helper().getUser().getUsername(),
                (String) TestHelper.getUserCredentials().get(Attributes.PASSWORD));
        ClientResponse response = resource().path(getAPIVersion()).path("oauth2")
                .path("register")
                .queryParam("redirect_url", "korap.ids-mannheim.de/redirect")
                .queryParam("application_name", "Kustvakt test")
                .header("Host", "korap.ids-mannheim.de")
                .header(Attributes.AUTHORIZATION, auth)
                .post(ClientResponse.class);

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));


        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
    }


    @Test
    @Ignore
    public void testRevokeClient () throws ClientHandlerException, UniformInterfaceException, KustvaktException {
        ClientResponse response = resource().path(getAPIVersion()).path("oauth2")
                .path("register")
                .queryParam("redirect_url", "korap.ids-mannheim.de/redirect")
                .header("Host", "korap.ids-mannheim.de")
                .post(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));

    }


    @Test
    @Ignore
    public void authenticate () throws KustvaktException {
        Map<String, Object> cred = TestHelper.getUserCredentials();
        String enc = BasicHttpAuth.encode((String) cred.get(Attributes.USERNAME), (String) cred.get(Attributes.PASSWORD));
        ClientResponse response = resource().path(getAPIVersion()).path("oauth2")
                .path("register")
                .queryParam("redirect_url", "korap.ids-mannheim.de/redirect")
                .header("Host", "korap.ids-mannheim.de")
                .header(Attributes.AUTHORIZATION, enc)
                .post(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String e = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(e);

        String cl_s = node.path("client_secret").asText();
        String cl_id = node.path("client_id").asText();

        response = resource().path(getAPIVersion()).path("oauth2").path("authorize")
                .queryParam("client_id", cl_id)
                .queryParam("client_secret", cl_s)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", "korap.ids-mannheim.de/redirect")
                //                .header(Attributes.AUTHORIZATION, enc)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(ClientResponse.class);

        e = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        node = JsonUtils.readTree(e);

        response = resource().path(getAPIVersion()).path("oauth2").path("authorize")
                .queryParam("code", node.path("authorization_code").asText())
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", cl_id)
                .queryParam("client_secret", cl_s).post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
    }

}
