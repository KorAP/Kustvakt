package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jetty.http.HttpHeaders;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.spi.container.ContainerRequest;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.AuthenticationType;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VirtualCorpusServiceTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;

    @Test
    //    @Ignore
    public void testStoreVC () throws KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVATE\",\"createdBy\": "
                        + "\"test class\",\"collectionQuery\": \"corpusSigle=GOE\"}";

        ClientResponse response = resource().path("vc").path("store").header(
                Attributes.AUTHORIZATION,
                handler.createAuthorizationHeader(AuthenticationType.BASIC,
                        "kustvakt", "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(json)
                .post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        System.out.println(entity);
    }

    @Test
    public void testStoreVCUnauthorized () throws KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVATE\",\"createdBy\": "
                        + "\"test class\",\"collectionQuery\": \"pubDate eq 1982\"}";

        ClientResponse response = resource().path("vc").path("store")
                .entity(json).post(ClientResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        
        Set<Entry<String, List<String>>> headers =
                response.getHeaders().entrySet();

        for (Entry<String, List<String>> header : headers) {
            if (header.getKey().equals(ContainerRequest.WWW_AUTHENTICATE)) {
                assertEquals("ldap realm=\"Kustvakt\"",
                        header.getValue().get(0));
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
//        System.out.println(entity);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertTrue(node.at("/errors/0/1").asText().startsWith(
                "Cannot deserialize value of type `de.ids_mannheim.korap.constant."
                        + "VirtualCorpusType` from String \"PRIVAT\": value not one of "
                        + "declared Enum instance names"));
    }
}
