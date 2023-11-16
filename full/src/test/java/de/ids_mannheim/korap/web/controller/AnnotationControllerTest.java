package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

public class AnnotationControllerTest extends SpringJerseyTest {

    @Test
    public void testAnnotationLayers () throws KustvaktException {
        Response response = target().path(API_VERSION).path("annotation")
                .path("layers").request().get();
        String entity = response.readEntity(String.class);
        JsonNode n = JsonUtils.readTree(entity);
        assertEquals(31, n.size());
        n = n.get(0);
        assertEquals(1, n.get("id").asInt());
        // assertEquals("opennlp/p", n.get("code").asText());
        // assertEquals("p", n.get("layer").asText());
        // assertEquals("opennlp", n.get("foundry").asText());
        // assertNotNull(n.get("description"));
    }

    @Test
    public void testAnnotationFoundry () throws KustvaktException {
        String json = "{\"codes\":[\"opennlp/*\"], \"language\":\"en\"}";
        Response response = target().path(API_VERSION).path("annotation")
                .path("description").request().post(Entity.json(json));
        String entity = response.readEntity(String.class);
        JsonNode n = JsonUtils.readTree(entity);
        n = n.get(0);
        assertEquals(n.get("code").asText(), "opennlp");
        assertEquals(n.get("description").asText(), "OpenNLP");
        assertEquals(1, n.get("layers").size());
        n = n.get("layers").get(0);
        assertEquals(n.get("code").asText(), "p");
        assertEquals(n.get("description").asText(), "Part-of-Speech");
        assertEquals(52, n.get("keys").size());
        n = n.get("keys").get(0);
        assertEquals(n.get("code").asText(), "ADJA");
        assertEquals(n.get("description").asText(), "Attributive Adjective");
        assertTrue(n.get("values") == null);
    }

    @Test
    public void testAnnotationValues () throws KustvaktException {
        String json = "{\"codes\":[\"mate/m\"], \"language\":\"en\"}";
        Response response = target().path(API_VERSION).path("annotation")
                .path("description").request().post(Entity.json(json));
        String entity = response.readEntity(String.class);
        JsonNode n = JsonUtils.readTree(entity);
        n = n.get(0);
        assertEquals(n.get("code").asText(), "mate");
        assertEquals(n.get("description").asText(), "Mate");
        assertEquals(1, n.get("layers").size());
        n = n.get("layers").get(0);
        assertEquals(n.get("code").asText(), "m");
        assertEquals(n.get("description").asText(), "Morphology");
        assertEquals(8, n.get("keys").size());
        n = n.get("keys").get(1);
        assertEquals(n.get("code").asText(), "case");
        assertEquals(n.get("description").asText(), "Case");
        assertEquals(5, n.get("values").size());
        n = n.get("values");
        Iterator<Entry<String, JsonNode>> fields = n.fields();
        Entry<String, JsonNode> e = fields.next();
        assertEquals(e.getKey(), "*");
        assertEquals(e.getValue().asText(), "Undefined");
    }
}
