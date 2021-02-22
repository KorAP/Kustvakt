package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.NamedVCLoader;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.dao.QueryDao;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;
import net.sf.ehcache.CacheManager;

public class VCReferenceTest extends SpringJerseyTest {

    @Autowired
    private NamedVCLoader vcLoader;
    @Autowired
    private QueryDao dao;

    @Test
    public void testRefPredefinedVC ()
            throws KustvaktException, IOException, QueryException {
        testSearchWithoutVCRefOr();
        testSearchWithoutVCRefAnd();

        KrillCollection.cache = CacheManager.newInstance().getCache("named_vc");
        vcLoader.loadVCToCache("named-vc1", "/vc/named-vc1.jsonld");
        testStatisticsWithVCReference();

        // TODO: test auto-caching (disabled in krill)
        vcLoader.loadVCToCache("named-vc2", "/vc/named-vc2.jsonld");
        testSearchWithVCRefNotEqual();

        // retrieve from cache
        testSearchWithVCRefEqual();
        testSearchWithVCRefNotEqual();

        KrillCollection.cache.removeAll();
        QueryDO vc = dao.retrieveQueryByName("named-vc1", "system");
        dao.deleteQuery(vc);
        vc = dao.retrieveQueryByName("named-vc2", "system");
        dao.deleteQuery(vc);
    }

    private void testSearchWithoutVCRefOr () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq",
                        "textSigle=\"GOE/AGF/00000\" | textSigle=\"GOE/AGA/01784\"")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
    }

    private void testSearchWithoutVCRefAnd () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq",
                        "textSigle!=\"GOE/AGI/04846\" & textSigle!=\"GOE/AGA/01784\"")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
    }

    public void testSearchWithVCRefEqual () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system/named-vc1\"")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
    }

    public void testSearchWithVCRefNotEqual () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo named-vc2")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
    }

    public void testStatisticsWithVCReference () throws KustvaktException {
        String corpusQuery = "availability = /CC-BY.*/ & referTo named-vc1";
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics").queryParam("corpusQuery", corpusQuery)
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(2, node.at("/documents").asInt());
    }

    @Test
    public void testRefVCNotExist () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"username/vc1\"")
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
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("guest", node.at("/errors/0/2").asText());
    }
    
    @Test
    public void testSearchWithPublishedVCRefGuest () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"marlin/published-vc\"")
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
    public void testSearchWithPublishedVCRef () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"marlin/published-vc\"")
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
