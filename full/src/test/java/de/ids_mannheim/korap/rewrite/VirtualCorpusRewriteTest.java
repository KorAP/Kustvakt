package de.ids_mannheim.korap.rewrite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
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

/**
 * @author margaretha
 *
 */
public class VirtualCorpusRewriteTest extends SpringJerseyTest {

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
                .queryParam("cq", "referTo named-vc1")
                .request()
                .get();

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
                .queryParam("cq", "referTo \"system/named-vc1\"")
                .request()
                .get();

        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals("koral:docGroup", node.at("/@type").asText());

        node = node.at("/operands/1/rewrites");
        assertEquals(2, node.size());
        assertEquals("operation:deletion", node.at("/0/operation").asText());
        assertEquals("operation:insertion", node.at("/1/operation").asText());

    }

    @Test
    public void testRewriteFreeAndSystemVCRef ()
            throws KustvaktException, Exception {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system-vc\"")
                .request()
                .get();

        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");

        assertEquals("koral:docGroup", node.at("/@type").asText());
        assertEquals("koral:doc", node.at("/operands/0/@type").asText());

        assertEquals("koral:doc", node.at("/operands/1/@type").asText());
        assertEquals("GOE", node.at("/operands/1/value").asText());
        assertEquals("corpusSigle", node.at("/operands/1/key").asText());

        node = node.at("/operands/1/rewrites");
        assertEquals(3, node.size());
        assertEquals("operation:deletion", node.at("/0/operation").asText());
        assertEquals("operation:deletion", node.at("/1/operation").asText());
        assertEquals("operation:insertion", node.at("/2/operation").asText());
    }

    @Test
    public void testRewritePubAndSystemVCRef () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system/system-vc\"")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("user", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals("koral:docGroup", node.at("/@type").asText());
        assertEquals("koral:docGroup", node.at("/operands/0/@type").asText());

        node = node.at("/operands/1/rewrites");
        assertEquals(3, node.size());
        assertEquals("operation:deletion", node.at("/0/operation").asText());
        assertEquals("operation:deletion", node.at("/1/operation").asText());
        assertEquals("operation:insertion", node.at("/2/operation").asText());
    }

    @Test
    public void testRewriteWithDoryVCRef ()
            throws KustvaktException, IOException, QueryException {

        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Fisch").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"dory/dory-vc\"")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .get();

        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals("koral:docGroup", node.at("/@type").asText());
        node = node.at("/operands/1/rewrites");
        assertEquals(3, node.size());
    }
}
