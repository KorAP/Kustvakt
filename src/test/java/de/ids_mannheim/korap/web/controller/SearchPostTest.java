package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/** Reactivated Search Post tests. 
 *  [AI-assisted]
 * 
 */
public class SearchPostTest extends SpringJerseyTest {

    private double apiVersion = Double.parseDouble(API_VERSION.substring(1));
    
    private String createJsonQuery () {
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery("[orth=der]", "poliqarp");
        s.setCollection("corpusSigle=GOE");
        s.setQuery("Wasser", "poliqarp");
        return s.toJSON();
    }
    
    @Test
    public void testSearchSimpleCQL () throws KustvaktException {
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery("(der) or (das)", "CQL");
        String query = s.toJSON();
        Response response = target().path(API_VERSION).path("search").request()
                .post(Entity.json(query));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertFalse(node.at("/query").isMissingNode());
        assertFalse(node.at(CORPUS_PATH).isMissingNode());
        assertFalse(node.at("/matches").isMissingNode());
    }

    @Test
    public void testSearchRawQuery () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").request()
                .post(Entity.json(createJsonQuery()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        
        // Check query structure
        assertFalse(node.at("/query").isMissingNode());
        assertEquals("koral:token", node.at("/query/@type").asText());
        
        // Check query wrap structure
        assertFalse(node.at("/query/wrap").isMissingNode());
        assertEquals("koral:term", node.at("/query/wrap/@type").asText());
        assertEquals("match:eq", node.at("/query/wrap/match").asText());
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertEquals("Wasser", node.at("/query/wrap/key").asText());
        assertEquals("opennlp", node.at("/query/wrap/foundry").asText());
        
        // Check query wrap rewrites
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
        assertTrue(node.at("/query/wrap/rewrites").isArray());
        assertNotEquals(0, node.at("/query/wrap/rewrites").size());
        assertEquals("koral:rewrite", node.at("/query/wrap/rewrites/0/@type").asText());
        assertEquals("Kustvakt", node.at("/query/wrap/rewrites/0/src").asText());
        assertEquals("Kustvakt", node.at("/query/wrap/rewrites/0/editor").asText());
        assertEquals("operation:injection", node.at("/query/wrap/rewrites/0/operation").asText());
        assertEquals("foundry", node.at("/query/wrap/rewrites/0/scope").asText());
        assertNotNull(node.at("/query/wrap/rewrites/0/_comment").asText());
        
        // Check matches and metadata
        assertNotEquals(0, node.path("matches").size());
        assertFalse(node.at("/meta").isMissingNode());
        assertTrue(node.at("/meta/count").isInt());
        assertNotNull(node.at("/meta/count").asInt());
        
        // Check corpus structure
        assertFalse(node.at(CORPUS_PATH).isMissingNode());
        assertEquals("koral:docGroup", node.at(CORPUS_PATH+"/@type").asText());
        assertEquals("operation:and", node.at(CORPUS_PATH+"/operation").asText());
        
        // Check corpus operands
        assertTrue(node.at(CORPUS_PATH+"/operands").isArray());
        assertEquals(2, node.at(CORPUS_PATH+"/operands").size());
        
        // Check first operand (availability)
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/operands/0/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/operands/0/match").asText());
        assertEquals("type:regex", node.at(CORPUS_PATH+"/operands/0/type").asText());
        assertEquals("CC.*", node.at(CORPUS_PATH+"/operands/0/value").asText());
        assertEquals("availability", node.at(CORPUS_PATH+"/operands/0/key").asText());
        
        // Check second operand (corpusSigle)
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/operands/1/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/operands/1/match").asText());
        assertEquals("GOE", node.at(CORPUS_PATH+"/operands/1/value").asText());
        assertEquals("corpusSigle", node.at(CORPUS_PATH+"/operands/1/key").asText());
        
        // Check corpus rewrites
        assertFalse(node.at(CORPUS_PATH+"/rewrites").isMissingNode());
        assertTrue(node.at(CORPUS_PATH+"/rewrites").isArray());
        assertNotEquals(0, node.at(CORPUS_PATH+"/rewrites").size());
        assertEquals("koral:rewrite", node.at(CORPUS_PATH+"/rewrites/0/@type").asText());
        assertEquals("Kustvakt", node.at(CORPUS_PATH+"/rewrites/0/src").asText());
        assertEquals("Kustvakt", node.at(CORPUS_PATH+"/rewrites/0/editor").asText());
        assertEquals("operation:override", node.at(CORPUS_PATH+"/rewrites/0/operation").asText());
        assertEquals(freeCorpusAccess, node.at(CORPUS_PATH+"/rewrites/0/_comment").asText());
        
        // Check rewrite original
        assertFalse(node.at(CORPUS_PATH+"/rewrites/0/original").isMissingNode());
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/rewrites/0/original/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/rewrites/0/original/match").asText());
        assertEquals("GOE", node.at(CORPUS_PATH+"/rewrites/0/original/value").asText());
        assertEquals("corpusSigle", node.at(CORPUS_PATH+"/rewrites/0/original/key").asText());
    }

    @Test
    public void testSearchPostAll () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "10.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .post(Entity.json(createJsonQuery()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        
        // Check corpus structure
        assertFalse(node.at(CORPUS_PATH).isMissingNode());
        assertEquals("koral:docGroup", node.at(CORPUS_PATH+"/@type").asText());
        assertEquals("operation:and", node.at(CORPUS_PATH+"/operation").asText());
        
        // Check root operands (2 operands)
        assertTrue(node.at(CORPUS_PATH+"/operands").isArray());
        assertEquals(2, node.at(CORPUS_PATH+"/operands").size());
        
        // Check first operand (nested availability docGroup)
        assertEquals("koral:docGroup", node.at(CORPUS_PATH+"/operands/0/@type").asText());
        assertEquals("operation:or", node.at(CORPUS_PATH+"/operands/0/operation").asText());
        
        // Check first operand's operands (nested availability checks)
        assertTrue(node.at(CORPUS_PATH+"/operands/0/operands").isArray());
        assertEquals(2, node.at(CORPUS_PATH+"/operands/0/operands").size());
        
        // Check CC.* availability
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/operands/0/operands/0/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/operands/0/operands/0/match").asText());
        assertEquals("type:regex", node.at(CORPUS_PATH+"/operands/0/operands/0/type").asText());
        assertEquals("CC.*", node.at(CORPUS_PATH+"/operands/0/operands/0/value").asText());
        assertEquals("availability", node.at(CORPUS_PATH+"/operands/0/operands/0/key").asText());
        
        // Check second level nested docGroup (ACA.* and QAO checks)
        assertEquals("koral:docGroup", node.at(CORPUS_PATH+"/operands/0/operands/1/@type").asText());
        assertEquals("operation:or", node.at(CORPUS_PATH+"/operands/0/operands/1/operation").asText());
        
        // Check second level operands
        assertTrue(node.at(CORPUS_PATH+"/operands/0/operands/1/operands").isArray());
        assertEquals(2, node.at(CORPUS_PATH+"/operands/0/operands/1/operands").size());
        
        // Check ACA.* availability
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/0/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/0/match").asText());
        assertEquals("type:regex", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/0/type").asText());
        assertEquals("ACA.*", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/0/value").asText());
        assertEquals("availability", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/0/key").asText());
        
        // Check third level nested docGroup (QAO checks)
        assertEquals("koral:docGroup", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/@type").asText());
        assertEquals("operation:or", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operation").asText());
        
        // Check third level operands
        assertTrue(node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands").isArray());
        assertEquals(2, node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands").size());
        
        // Check QAO-NC availability
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands/0/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands/0/match").asText());
        assertEquals("type:regex", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands/0/type").asText());
        assertEquals("QAO-NC", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands/0/value").asText());
        assertEquals("availability", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands/0/key").asText());
        
        // Check QAO-NC-LOC:ids.* availability
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands/1/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands/1/match").asText());
        assertEquals("type:regex", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands/1/type").asText());
        assertEquals("QAO-NC-LOC:ids.*", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands/1/value").asText());
        assertEquals("availability", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/operands/1/key").asText());
        
        // Check second root operand (corpusSigle)
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/operands/1/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/operands/1/match").asText());
        assertEquals("GOE", node.at(CORPUS_PATH+"/operands/1/value").asText());
        assertEquals("corpusSigle", node.at(CORPUS_PATH+"/operands/1/key").asText());
        
        // Check corpus rewrites
        assertFalse(node.at(CORPUS_PATH+"/rewrites").isMissingNode());
        assertTrue(node.at(CORPUS_PATH+"/rewrites").isArray());
        assertNotEquals(0, node.at(CORPUS_PATH+"/rewrites").size());
        assertEquals("koral:rewrite", node.at(CORPUS_PATH+"/rewrites/0/@type").asText());
        assertEquals("Kustvakt", node.at(CORPUS_PATH+"/rewrites/0/src").asText());
        assertEquals("Kustvakt", node.at(CORPUS_PATH+"/rewrites/0/editor").asText());
        assertEquals("operation:override", node.at(CORPUS_PATH+"/rewrites/0/operation").asText());
        assertEquals(allCorpusAccess, node.at(CORPUS_PATH+"/rewrites/0/_comment").asText());
        
        // Check rewrite original
        assertFalse(node.at(CORPUS_PATH+"/rewrites/0/original").isMissingNode());
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/rewrites/0/original/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/rewrites/0/original/match").asText());
        assertEquals("GOE", node.at(CORPUS_PATH+"/rewrites/0/original/value").asText());
        assertEquals("corpusSigle", node.at(CORPUS_PATH+"/rewrites/0/original/key").asText());
    }

    @Test
    public void testSearchPostPublic () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .post(Entity.json(createJsonQuery()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        
        // Check corpus structure
        assertFalse(node.at(CORPUS_PATH).isMissingNode());
        assertEquals("koral:docGroup", node.at(CORPUS_PATH+"/@type").asText());
        assertEquals("operation:and", node.at(CORPUS_PATH+"/operation").asText());
        
        // Check root operands (2 operands)
        assertTrue(node.at(CORPUS_PATH+"/operands").isArray());
        assertEquals(2, node.at(CORPUS_PATH+"/operands").size());
        
        // Check first operand (nested availability docGroup)
        assertEquals("koral:docGroup", node.at(CORPUS_PATH+"/operands/0/@type").asText());
        assertEquals("operation:or", node.at(CORPUS_PATH+"/operands/0/operation").asText());
        
        // Check first operand's operands (nested availability checks)
        assertTrue(node.at(CORPUS_PATH+"/operands/0/operands").isArray());
        assertEquals(2, node.at(CORPUS_PATH+"/operands/0/operands").size());
        
        // Check CC.* availability
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/operands/0/operands/0/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/operands/0/operands/0/match").asText());
        assertEquals("type:regex", node.at(CORPUS_PATH+"/operands/0/operands/0/type").asText());
        assertEquals("CC.*", node.at(CORPUS_PATH+"/operands/0/operands/0/value").asText());
        assertEquals("availability", node.at(CORPUS_PATH+"/operands/0/operands/0/key").asText());
        
        // Check second level nested docGroup (ACA.* and QAO checks)
        assertEquals("koral:docGroup", node.at(CORPUS_PATH+"/operands/0/operands/1/@type").asText());
        assertEquals("operation:or", node.at(CORPUS_PATH+"/operands/0/operands/1/operation").asText());
        
        // Check second level operands
        assertTrue(node.at(CORPUS_PATH+"/operands/0/operands/1/operands").isArray());
        assertEquals(2, node.at(CORPUS_PATH+"/operands/0/operands/1/operands").size());
        
        // Check ACA.* availability
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/0/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/0/match").asText());
        assertEquals("type:regex", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/0/type").asText());
        assertEquals("ACA.*", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/0/value").asText());
        assertEquals("availability", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/0/key").asText());
        
        // Check QAO-NC availability
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/match").asText());
        assertEquals("type:regex", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/type").asText());
        assertEquals("QAO-NC", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/value").asText());
        assertEquals("availability", node.at(CORPUS_PATH+"/operands/0/operands/1/operands/1/key").asText());
        
        // Check second root operand (corpusSigle)
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/operands/1/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/operands/1/match").asText());
        assertEquals("GOE", node.at(CORPUS_PATH+"/operands/1/value").asText());
        assertEquals("corpusSigle", node.at(CORPUS_PATH+"/operands/1/key").asText());
        
        // Check corpus rewrites
        assertFalse(node.at(CORPUS_PATH+"/rewrites").isMissingNode());
        assertTrue(node.at(CORPUS_PATH+"/rewrites").isArray());
        assertNotEquals(0, node.at(CORPUS_PATH+"/rewrites").size());
        assertEquals("koral:rewrite", node.at(CORPUS_PATH+"/rewrites/0/@type").asText());
        assertEquals("Kustvakt", node.at(CORPUS_PATH+"/rewrites/0/src").asText());
        assertEquals("Kustvakt", node.at(CORPUS_PATH+"/rewrites/0/editor").asText());
        assertEquals("operation:override", node.at(CORPUS_PATH+"/rewrites/0/operation").asText());
        assertEquals(publicCorpusAccess, node.at(CORPUS_PATH+"/rewrites/0/_comment").asText());
        
        // Check rewrite original
        assertFalse(node.at(CORPUS_PATH+"/rewrites/0/original").isMissingNode());
        assertEquals("koral:doc", node.at(CORPUS_PATH+"/rewrites/0/original/@type").asText());
        assertEquals("match:eq", node.at(CORPUS_PATH+"/rewrites/0/original/match").asText());
        assertEquals("GOE", node.at(CORPUS_PATH+"/rewrites/0/original/value").asText());
        assertEquals("corpusSigle", node.at(CORPUS_PATH+"/rewrites/0/original/key").asText());
    }

}
