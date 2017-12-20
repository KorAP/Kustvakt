package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jetty.http.HttpHeaders;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.spi.container.ContainerRequest;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.AuthenticationScheme;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VirtualCorpusServiceTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;

    @Test
    public void testRetrieveUserVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("vc").path("user")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
//        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(3, node.size());
    }
    @Test
    public void testStoreVC () throws KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVATE\",\"createdBy\": "
                        + "\"test class\",\"collectionQuery\": \"corpusSigle=GOE\"}";

        ClientResponse response = resource().path("vc").path("store")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("test class",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(json)
                .post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = resource().path("vc").path("user")
                .queryParam("username", "test class")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("test class",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                
                .get(ClientResponse.class);
        entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());   
//        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
        assertEquals("new vc", node.get(1).get("name").asText());
    }

    @Test
    public void testStoreVCWithExpiredToken ()
            throws IOException, KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVATE\",\"createdBy\": "
                        + "\"test class\",\"collectionQuery\": \"corpusSigle=GOE\"}";

        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("test-user.token");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String authToken = reader.readLine();

        ClientResponse response = resource().path("vc").path("store")
                .header(Attributes.AUTHORIZATION,
                        AuthenticationScheme.API.displayName() + " "
                                + authToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(json)
                .post(ClientResponse.class);
        String entity = response.getEntity(String.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.EXPIRED, node.at("/errors/0/0").asInt());
        assertEquals("Authentication token is expired",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testStoreVCUnauthorized () throws KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVATE\",\"createdBy\": "
                        + "\"test class\",\"collectionQuery\": \"pubDate eq 1982\"}";

        ClientResponse response = resource().path("vc").path("store")
                .entity(json).post(ClientResponse.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        Set<Entry<String, List<String>>> headers =
                response.getHeaders().entrySet();

        for (Entry<String, List<String>> header : headers) {
            if (header.getKey().equals(ContainerRequest.WWW_AUTHENTICATE)) {
                assertEquals("Api realm=\"Kustvakt\"",
                        header.getValue().get(0));
                assertEquals("Session realm=\"Kustvakt\"",
                        header.getValue().get(1));
                assertEquals("Bearer realm=\"Kustvakt\"",
                        header.getValue().get(2));
                assertEquals("Basic realm=\"Kustvakt\"",
                        header.getValue().get(3));
            }
        }

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Operation is not permitted for user: guest",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testStoreVCWithWrongType () throws KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVAT\",\"createdBy\": "
                        + "\"test class\",\"collectionQuery\": \"pubDate eq 1982\"}";

        ClientResponse response = resource().path("vc").path("store")
                .entity(json).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
                System.out.println(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertTrue(node.at("/errors/0/1").asText().startsWith(
                "Cannot deserialize value of type `de.ids_mannheim.korap.constant."
                        + "VirtualCorpusType` from String \"PRIVAT\": value not one of "
                        + "declared Enum instance names"));
    }
}
