package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class MetadataControllerTest extends SpringJerseyTest {

    @Test
    public void testRetrieveMetadataWithField () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("corpus")
                .path("GOE").path("AGA").path("01784")
                .queryParam("fields", "author").get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals("author", node.at("/document/fields/0/key").asText());
    }
    

    @Test
    public void testRetrieveMetadataWithMultipleFields () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("corpus")
                .path("GOE").path("AGA").path("01784")
                .queryParam("fields", "author,title").get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals("author", node.at("/document/fields/0/key").asText());
        assertEquals("title", node.at("/document/fields/1/key").asText());
    }    
    
    @Test
    public void testFreeMetadata () throws KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("corpus")
                .path("GOE").path("AGA").path("01784")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertTrue(!node.at("/document").isMissingNode());

    }

    // EM: currently all metadata are allowed
    @Test
    @Ignore
    public void testMetadataUnauthorized () throws KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("corpus")
                .path("GOE").path("AGI").path("04846")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Retrieving resource with ID "
                        + "GOE/AGI/04846 is not allowed.",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testMetadataWithAuthentication () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("corpus")
                .path("GOE").path("AGI").path("04846")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "172.27.0.32")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
    }

    @Test
    public void testMetadataAvailabilityAll () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("corpus")
                .path("GOE").path("AGI").path("00000")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "10.27.0.32")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
    }

    // EM: currently all metadata are allowed
    @Test
    @Ignore
    public void testMetadataAvailabilityAllUnauthorized ()
            throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("corpus")
                .path("GOE").path("AGD").path("00000")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "170.27.0.32")
                .get(ClientResponse.class);
        
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Retrieving resource with ID "
                        + "GOE/AGD/00000 is not allowed.",
                node.at("/errors/0/1").asText());
    }
}
