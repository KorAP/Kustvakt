package de.ids_mannheim.korap.web.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.LiteJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class TestDummyServices extends LiteJerseyTest {

    @Test
    public void testGlemmService () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("test")
                .path("glemm").post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/query/wrap/key").size());
        node = node.at("/query/wrap/rewrites");
        assertEquals("Glemm", node.at("/0/src").asText());
        assertEquals("operation:override", node.at("/0/operation").asText());
        assertEquals("key", node.at("/0/scope").asText());
    }
}
