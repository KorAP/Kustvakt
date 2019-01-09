package de.ids_mannheim.korap.rewrite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.NamedVCLoader;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.dao.VirtualCorpusDao;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;
import net.sf.ehcache.CacheManager;

/**
 * @author margaretha
 *
 */
public class VirtualCorpusRewriteTest extends SpringJerseyTest {

    @Autowired
    private NamedVCLoader vcLoader;
    @Autowired
    private VirtualCorpusDao dao;

    @Test
    public void testCachedVCRef ()
            throws KustvaktException, IOException, QueryException {
        KrillCollection.cache = CacheManager.newInstance().getCache("named_vc");
        vcLoader.loadVCToCache("named-vc1", "/vc/named-vc1.jsonld");

        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo named-vc1")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");

        assertEquals("koral:docGroup", node.at("/@type").asText());
        assertTrue(node.at("/operands/1/rewrites").isMissingNode());

        testCachedVCRefWithUsername();

        KrillCollection.cache.removeAll();
        VirtualCorpus vc = dao.retrieveVCByName("named-vc1", "system");
        dao.deleteVirtualCorpus(vc);
    }

    private void testCachedVCRefWithUsername ()
            throws KustvaktException, IOException, QueryException {

        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system/named-vc1\"")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
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
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system VC\"")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
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
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system/system VC\"")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("user", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
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

        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "Fisch").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"dory/dory VC\"")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals("koral:docGroup", node.at("/@type").asText());
        node = node.at("/operands/1/rewrites");
        assertEquals(3, node.size());
    }
}
