package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

// EM: The API is disabled
@Disabled
public class QuerySerializationControllerTest extends SpringJerseyTest {

    @Test
    public void testQuerySerializationFilteredPublic ()
            throws KustvaktException {
        Response response = target().path(API_VERSION)
                .path("corpus/WPD13/query").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(node.at("/collection/key").asText(), "corpusSigle");
        assertEquals(node.at("/collection/value").asText(), "WPD13");
    }

    @Test
    public void testQuerySerializationUnexistingResource ()
            throws KustvaktException {
        Response response = target().path(API_VERSION)
                .path("corpus/ZUW19/query").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .request().method("GET");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(),
                "[Cannot found public Corpus with ids: [ZUW19]]");
    }

    @Test
    public void testQuerySerializationWithNonPublicCorpus ()
            throws KustvaktException {
        Response response = target().path(API_VERSION)
                .path("corpus/BRZ10/query").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .request().method("GET");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(),
                "[Cannot found public Corpus with ids: [BRZ10]]");
    }

    @Test
    public void testQuerySerializationWithAuthentication ()
            throws KustvaktException {
        Response response = target().path(API_VERSION)
                .path("corpus/BRZ10/query").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/key").asText(), "corpusSigle");
        assertEquals(node.at("/collection/value").asText(), "BRZ10");
    }

    @Test
    public void testQuerySerializationWithNewCollection ()
            throws KustvaktException {
        // Add Virtual Collection
        Response response = target().path(API_VERSION).path("virtualcollection")
                .queryParam("filter", "false")
                .queryParam("query",
                        "creationDate since 1775 & corpusSigle=GOE")
                .queryParam("name", "Weimarer Werke")
                .queryParam("description", "Goethe-Werke in Weimar (seit 1775)")
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .post(Entity.json(""));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals(node.path("name").asText(), "Weimarer Werke");
        // Get virtual collections
        response = target().path(API_VERSION).path("collection").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        ent = response.readEntity(String.class);
        node = JsonUtils.readTree(ent);
        assertNotNull(node);
        Iterator<JsonNode> it = node.elements();
        String id = null;
        while (it.hasNext()) {
            JsonNode next = (JsonNode) it.next();
            if ("Weimarer Werke".equals(next.path("name").asText()))
                id = next.path("id").asText();
        }
        assertNotNull(id);
        assertFalse(id.isEmpty());
        // query serialization service
        response = target().path(API_VERSION).path("collection").path(id)
                .path("query").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        ent = response.readEntity(String.class);
        node = JsonUtils.readTree(ent);
        assertNotNull(node);
        // System.out.println("NODE " + ent);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operands/0/@type").asText(),
                "koral:doc");
        assertEquals(node.at("/collection/operands/0/key").asText(),
                "creationDate");
        assertEquals(node.at("/collection/operands/0/value").asText(), "1775");
        assertEquals(node.at("/collection/operands/0/type").asText(),
                "type:date");
        assertEquals(node.at("/collection/operands/0/match").asText(),
                "match:geq");
        assertEquals(node.at("/collection/operands/1/@type").asText(),
                "koral:doc");
        assertEquals(node.at("/collection/operands/1/key").asText(),
                "corpusSigle");
        assertEquals(node.at("/collection/operands/1/value").asText(), "GOE");
        assertEquals(node.at("/collection/operands/1/match").asText(),
                "match:eq");
    }

    @Test
    public void testQuerySerializationOfVirtualCollection ()
            throws KustvaktException {
        Response response = target().path(API_VERSION)
                .path("collection/GOE-VC/query").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(node.at("/collection/operands/0/@type").asText(),
                "koral:doc");
        assertEquals(node.at("/collection/operands/0/key").asText(),
                "corpusSigle");
        assertEquals(node.at("/collection/operands/0/value").asText(), "GOE");
        assertEquals(node.at("/collection/operands/1/@type").asText(),
                "koral:doc");
        assertEquals(node.at("/collection/operands/1/key").asText(),
                "creationDate");
        assertEquals(node.at("/collection/operands/1/value").asText(),
                "1810-01-01");
    }

    @Test
    public void testMetaQuerySerialization () throws KustvaktException {
        Response response = target().path(API_VERSION).path("query")
                .queryParam("context", "sentence").queryParam("count", "20")
                .queryParam("page", "5").queryParam("cutoff", "true")
                .queryParam("q", "[pos=ADJA]").queryParam("ql", "poliqarp")
                .request().method("GET");
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/meta/context").asText(), "sentence");
        assertEquals(20, node.at("/meta/count").asInt());
        assertEquals(5, node.at("/meta/startPage").asInt());
        assertEquals(true, node.at("/meta/cutOff").asBoolean());
        assertEquals(node.at("/query/wrap/@type").asText(), "koral:term");
        assertEquals(node.at("/query/wrap/layer").asText(), "pos");
        assertEquals(node.at("/query/wrap/match").asText(), "match:eq");
        assertEquals(node.at("/query/wrap/key").asText(), "ADJA");
    }

    @Test
    public void testMetaQuerySerializationWithOffset ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("query")
                .queryParam("context", "sentence").queryParam("count", "20")
                .queryParam("page", "5").queryParam("offset", "2")
                .queryParam("cutoff", "true").queryParam("q", "[pos=ADJA]")
                .queryParam("ql", "poliqarp").request().method("GET");
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/meta/context").asText(), "sentence");
        assertEquals(20, node.at("/meta/count").asInt());
        assertEquals(2, node.at("/meta/startIndex").asInt());
        assertEquals(true, node.at("/meta/cutOff").asBoolean());
    }
}
