package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

@ContextConfiguration("classpath:test-annotation-config.xml")
public class AnnotationControllerTest extends SpringJerseyTest {
    @Test
    public void testAnnotationLayers () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION)
                .path("annotation").path("layers").get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode n = JsonUtils.readTree(entity);

        assertEquals(31, n.size());
        n = n.get(0);
        assertEquals(1, n.get("id").asInt());
//        assertEquals("opennlp/p", n.get("code").asText());
//        assertEquals("p", n.get("layer").asText());
//        assertEquals("opennlp", n.get("foundry").asText());
//        assertNotNull(n.get("description"));
    }

    @Test
    public void testAnnotationFoundry () throws KustvaktException {
        ClientResponse response =
                resource().path(API_VERSION).path("annotation")
                        .path("description").type(MediaType.APPLICATION_JSON)
                        .entity("{\"codes\":[\"opennlp/*\"], \"language\":\"en\"}")
                        .post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode n = JsonUtils.readTree(entity);

        n = n.get(0);
        assertEquals("opennlp", n.get("code").asText());
        assertEquals("OpenNLP", n.get("description").asText());
        assertEquals(1, n.get("layers").size());

        n = n.get("layers").get(0);
        assertEquals("p", n.get("code").asText());
        assertEquals("Part-of-Speech", n.get("description").asText());
        assertEquals(52, n.get("keys").size());

        n = n.get("keys").get(0);
        assertEquals("ADJA", n.get("code").asText());
        assertEquals("Attributive Adjective", n.get("description").asText());
        assertTrue(n.get("values") == null);
    }

    @Test
    public void testAnnotationValues () throws KustvaktException {
        ClientResponse response =
                resource().path(API_VERSION).path("annotation")
                        .path("description").type(MediaType.APPLICATION_JSON)
                        .entity("{\"codes\":[\"mate/m\"], \"language\":\"en\"}")
                        .post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode n = JsonUtils.readTree(entity);

        n = n.get(0);
        assertEquals("mate", n.get("code").asText());
        assertEquals("Mate", n.get("description").asText());
        assertEquals(1, n.get("layers").size());

        n = n.get("layers").get(0);
        assertEquals("m", n.get("code").asText());
        assertEquals("Morphology", n.get("description").asText());
        assertEquals(8, n.get("keys").size());

        n = n.get("keys").get(1);
        assertEquals("case", n.get("code").asText());
        assertEquals("Case", n.get("description").asText());
        assertEquals(5, n.get("values").size());

        n = n.get("values");
        Iterator<Entry<String, JsonNode>> fields = n.fields();
        Entry<String, JsonNode> e = fields.next();
        assertEquals("*", e.getKey());
        assertEquals("Undefined", e.getValue().asText());
    }

}
