package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.NamedVCLoader;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.dao.QueryDao;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VCReferenceTest extends SpringJerseyTest {

    @Autowired
    private NamedVCLoader vcLoader;
    @Autowired
    private QueryDao dao;

    /**
     * VC data exists, but it has not been cached, so it is not found
     * in the DB.
     * 
     * @throws KustvaktException
     */
    @Test
    public void testRefVcNotPrecached () throws KustvaktException {
        JsonNode node = testSearchWithRef_VC1();
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Virtual corpus system/named-vc1 is not found.",
                node.at("/errors/0/1").asText());
        assertEquals("system/named-vc1", node.at("/errors/0/2").asText());
    }
    
    @Test
    public void testRefVcPrecached ()
            throws KustvaktException, IOException, QueryException {
        int numOfMatches = testSearchWithoutRef_VC1();
        vcLoader.loadVCToCache("named-vc1", "/vc/named-vc1.jsonld");
        assertTrue(VirtualCorpusCache.contains("named-vc1"));
        JsonNode node = testSearchWithRef_VC1();
        assertEquals(numOfMatches,node.at("/matches").size());
        
        testStatisticsWithRef();

        numOfMatches = testSearchWithoutRef_VC2();
        vcLoader.loadVCToCache("named-vc2", "/vc/named-vc2.jsonld");
        assertTrue(VirtualCorpusCache.contains("named-vc2"));
        node = testSearchWithRef_VC2();
        assertEquals(numOfMatches,node.at("/matches").size());
        
        VirtualCorpusCache.delete("named-vc2");
        assertFalse(VirtualCorpusCache.contains("named-vc2"));

        QueryDO vc = dao.retrieveQueryByName("named-vc1", "system");
        dao.deleteQuery(vc);
        vc = dao.retrieveQueryByName("named-vc1", "system");
        assertNull(vc);
        
        vc = dao.retrieveQueryByName("named-vc2", "system");
        dao.deleteQuery(vc);
        vc = dao.retrieveQueryByName("named-vc2", "system");
        assertNull(vc);
    }
    
    private int testSearchWithoutRef_VC1 () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq",
                        "textSigle=\"GOE/AGF/00000\" | textSigle=\"GOE/AGA/01784\"")
                .request()
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        int size = node.at("/matches").size();
        assertTrue(size > 0);
        return size;
    }

    private int testSearchWithoutRef_VC2 () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq",
                        "textSigle!=\"GOE/AGI/04846\" & textSigle!=\"GOE/AGA/01784\"")
                .request()
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        int size = node.at("/matches").size();
        assertTrue(size > 0);
        return size;
    }

    private JsonNode testSearchWithRef_VC1 () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system/named-vc1\"")
                .request()
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        return JsonUtils.readTree(ent);
    }

    private JsonNode testSearchWithRef_VC2 () throws KustvaktException {
        
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo named-vc2")
                .request()
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        return JsonUtils.readTree(ent);
    }

    @Test
    public void testStatisticsWithRef () throws KustvaktException {
        String corpusQuery = "availability = /CC-BY.*/ & referTo named-vc1";
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics").queryParam("corpusQuery", corpusQuery)
                .request()
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(2, node.at("/documents").asInt());
        
        VirtualCorpusCache.delete("named-vc1");
        assertFalse(VirtualCorpusCache.contains("named-vc1"));
    }

    @Test
    public void testRefVcNotExist () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"username/vc1\"")
                .request()
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("username/vc1", node.at("/errors/0/2").asText());
    }

    @Test
    public void testRefNotAuthorized() throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"dory/dory-vc\"")
                .request()
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("guest", node.at("/errors/0/2").asText());
    }
    
    @Test
    public void testSearchWithRefPublishedVcGuest () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"marlin/published-vc\"")
                .request()
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
        
        assertEquals("CC-BY.*", node.at("/collection/operands/0/value").asText());
        assertEquals("koral:doc", node.at("/collection/operands/1/@type").asText());
        assertEquals("GOE", node.at("/collection/operands/1/value").asText());
        assertEquals("corpusSigle", node.at("/collection/operands/1/key").asText());
        
        node = node.at("/collection/operands/1/rewrites");
        assertEquals(3, node.size());
        assertEquals("operation:deletion", node.at("/0/operation").asText());
        assertEquals("@type(koral:docGroupRef)", node.at("/0/scope").asText());
        assertEquals("operation:deletion", node.at("/1/operation").asText());
        assertEquals("ref(marlin/published-vc)", node.at("/1/scope").asText());
        assertEquals("operation:insertion", node.at("/2/operation").asText());
    }
    
    @Test
    public void testSearchWithRefPublishedVc () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"marlin/published-vc\"")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("squirt", "pass"))
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
        
        // check dory in the hidden group of the vc
        response = resource().path(API_VERSION).path("group")
                .path("list").path("system-admin")
                .queryParam("status", "HIDDEN")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(3, node.at("/0/id").asInt());
        
        String members = node.at("/0/members").toString();
        assertTrue(members.contains("\"userId\":\"squirt\""));
    } 
}
