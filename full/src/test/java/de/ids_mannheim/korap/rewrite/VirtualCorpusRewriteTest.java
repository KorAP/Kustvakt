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

import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.NamedVCLoader;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.dao.QueryDao;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author margaretha
 */
@DisplayName("Virtual Corpus Rewrite Test")
class VirtualCorpusRewriteTest extends SpringJerseyTest {

    @Autowired
    private NamedVCLoader vcLoader;

    @Autowired
    private QueryDao dao;

    @Test
    @DisplayName("Test Ref Cached VC")
    void testRefCachedVC() throws KustvaktException, IOException, QueryException {
        vcLoader.loadVCToCache("named-vc1", "/vc/named-vc1.jsonld");
        assertTrue(VirtualCorpusCache.contains("named-vc1"));
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("cq", "referTo named-vc1").request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals(node.at("/@type").asText(), "koral:docGroup");
        assertTrue(node.at("/operands/1/rewrites").isMissingNode());
        testRefCachedVCWithUsername();
        QueryDO vc = dao.retrieveQueryByName("named-vc1", "system");
        dao.deleteQuery(vc);
        vc = dao.retrieveQueryByName("named-vc1", "system");
        assertNull(vc);
        VirtualCorpusCache.delete("named-vc1");
        assertFalse(VirtualCorpusCache.contains("named-vc1"));
    }

    private void testRefCachedVCWithUsername() throws KustvaktException, IOException, QueryException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("cq", "referTo \"system/named-vc1\"").request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals(node.at("/@type").asText(), "koral:docGroup");
        node = node.at("/operands/1/rewrites");
        assertEquals(2, node.size());
        assertEquals(node.at("/0/operation").asText(), "operation:deletion");
        assertEquals(node.at("/1/operation").asText(), "operation:insertion");
    }

    @Test
    @DisplayName("Test Rewrite Free And System VC Ref")
    void testRewriteFreeAndSystemVCRef() throws KustvaktException, Exception {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("cq", "referTo \"system-vc\"").request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals(node.at("/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/operands/0/@type").asText(), "koral:doc");
        assertEquals(node.at("/operands/1/@type").asText(), "koral:doc");
        assertEquals(node.at("/operands/1/value").asText(), "GOE");
        assertEquals(node.at("/operands/1/key").asText(), "corpusSigle");
        node = node.at("/operands/1/rewrites");
        assertEquals(3, node.size());
        assertEquals(node.at("/0/operation").asText(), "operation:deletion");
        assertEquals(node.at("/1/operation").asText(), "operation:deletion");
        assertEquals(node.at("/2/operation").asText(), "operation:insertion");
    }

    @Test
    @DisplayName("Test Rewrite Pub And System VC Ref")
    void testRewritePubAndSystemVCRef() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("cq", "referTo \"system/system-vc\"").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("user", "pass")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals(node.at("/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/operands/0/@type").asText(), "koral:docGroup");
        node = node.at("/operands/1/rewrites");
        assertEquals(3, node.size());
        assertEquals(node.at("/0/operation").asText(), "operation:deletion");
        assertEquals(node.at("/1/operation").asText(), "operation:deletion");
        assertEquals(node.at("/2/operation").asText(), "operation:insertion");
    }

    @Test
    @DisplayName("Test Rewrite With Dory VC Ref")
    void testRewriteWithDoryVCRef() throws KustvaktException, IOException, QueryException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "Fisch").queryParam("ql", "poliqarp").queryParam("cq", "referTo \"dory/dory-vc\"").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("dory", "pass")).get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals(node.at("/@type").asText(), "koral:docGroup");
        node = node.at("/operands/1/rewrites");
        assertEquals(3, node.size());
    }
}
