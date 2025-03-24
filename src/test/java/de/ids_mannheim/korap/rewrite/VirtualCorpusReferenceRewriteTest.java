package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.dao.QueryDao;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.init.NamedVCLoader;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;

/**
 * @author margaretha
 */
public class VirtualCorpusReferenceRewriteTest extends SpringJerseyTest {

    @Autowired
    private NamedVCLoader vcLoader;

    @Autowired
    private QueryDao dao;

    @Test
    public void testRefCachedVC ()
            throws KustvaktException, IOException, QueryException {
        vcLoader.loadVCToCache("named-vc1", "/vc/named-vc1.jsonld");
        assertTrue(VirtualCorpusCache.contains("named-vc1"));
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo named-vc1").request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals("koral:docGroup", node.at("/@type").asText());
        assertTrue(node.at("/operands/1/rewrites").isMissingNode());
        
        testRefCachedVCWithUsername();
        QueryDO vc = dao.retrieveQueryByName("named-vc1", "system");
        dao.deleteQuery(vc);
        vc = dao.retrieveQueryByName("named-vc1", "system");
        assertNull(vc);
        VirtualCorpusCache.delete("named-vc1");
        assertFalse(VirtualCorpusCache.contains("named-vc1"));
    }

    private void testRefCachedVCWithUsername ()
            throws KustvaktException, IOException, QueryException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system/named-vc1\"").request()
                .get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals(node.at("/@type").asText(), "koral:docGroup");
        node = node.at("/operands/1/rewrites");
        
        assertEquals(1, node.size());
        assertEquals("koral:rewrite", node.at("/0/@type").asText());
        assertEquals("Kustvakt", node.at("/0/editor").asText());
        assertEquals("operation:override", node.at("/0/operation").asText());
        assertEquals("ref", node.at("/0/scope").asText());
        assertEquals("system/named-vc1", node.at("/0/original").asText());
    }

    @Test
    public void testRewriteFreeAndSystemVCRef ()
            throws KustvaktException, Exception {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system-vc\"").request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals("koral:docGroup", node.at("/@type").asText());
        assertEquals("operation:and", node.at("/operation").asText());
        
        assertEquals("koral:doc", node.at("/operands/0/@type").asText());
        assertEquals("CC.*", node.at("/operands/0/value").asText());
        
        assertEquals("koral:docGroupRef", node.at("/operands/1/@type").asText());
        assertEquals("system-vc", node.at("/operands/1/ref").asText());
//      
//        assertEquals("koral:doc", node.at("/operands/1/@type").asText());
//        assertEquals("GOE", node.at("/operands/1/value").asText());
//        assertEquals("match:eq", node.at("/operands/1/match").asText());
//        assertEquals("corpusSigle", node.at("/operands/1/key").asText());
//        
        System.out.println(node.toPrettyString());
        node = node.at("/rewrites/0");
        assertEquals("operation:override", node.at("/operation").asText());
        assertEquals("koral:docGroupRef", node.at("/original/@type").asText());
        assertEquals("system-vc", node.at("/original/ref").asText());    
    }

    @Test
    public void testRewritePubAndSystemVCRef () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system/system-vc\"").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("user", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals("koral:docGroup", node.at("/@type").asText());
        assertEquals("operation:and", node.at("/operation").asText());
        assertEquals(2, node.at("/operands").size());
        
        assertEquals("koral:docGroup", node.at("/operands/0/@type").asText());
        assertEquals("operation:or", node.at("/operands/0/operation").asText());
        
        JsonNode availability = node.at("/operands/0/operands");
        assertEquals(2, availability.size());
        assertEquals("CC.*", availability.at("/0/value").asText());
        assertEquals("operation:or", availability.at("/1/operation").asText());
        assertEquals("ACA.*", availability.at("/1/operands/0/value").asText());
        assertEquals("QAO-NC", availability.at("/1/operands/1/value").asText());
        
		assertEquals("koral:docGroupRef",
				node.at("/operands/1/@type").asText());
		assertEquals("system-vc", node.at("/operands/1/ref").asText());
//      
//        assertEquals("koral:doc", node.at("/operands/1/@type").asText());
//        assertEquals("GOE", node.at("/operands/1/value").asText());
//        assertEquals("match:eq", node.at("/operands/1/match").asText());
//        assertEquals("corpusSigle", node.at("/operands/1/key").asText());
        
        node = node.at("/rewrites/0");
        assertEquals("operation:override", node.at("/operation").asText());
        assertEquals("koral:docGroupRef", node.at("/original/@type").asText());
        assertEquals("system/system-vc", node.at("/original/ref").asText());
    }

    @Test
    public void testRewriteWithDoryVCRef ()
            throws KustvaktException, IOException, QueryException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Fisch").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"dory/dory-vc\"").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        
        assertEquals("koral:docGroup", node.at("/@type").asText());
        assertEquals("operation:and", node.at("/operation").asText());
        assertEquals("koral:doc", node.at("/operands/0/@type").asText());
        assertEquals("CC.*", node.at("/operands/0/value").asText());
        assertEquals("koral:docGroup", node.at("/operands/1/@type").asText());
        assertEquals(2, node.at("/operands/1/operands").size());
        
        node = node.at("/operands/1/rewrites/0");
        assertEquals("operation:override", node.at("/operation").asText());
        assertEquals("koral:docGroupRef", node.at("/original/@type").asText());
        assertEquals("dory/dory-vc", node.at("/original/ref").asText());
        
    }
}
