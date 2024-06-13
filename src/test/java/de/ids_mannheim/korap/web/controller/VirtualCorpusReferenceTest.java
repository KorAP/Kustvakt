package de.ids_mannheim.korap.web.controller;

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
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.init.NamedVCLoader;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class VirtualCorpusReferenceTest extends SpringJerseyTest {

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
        assertEquals(node.at("/errors/0/1").asText(),
                "Virtual corpus system/named-vc1 is not found.");
        assertEquals(node.at("/errors/0/2").asText(), "system/named-vc1");
    }

    @Test
    public void testRefVcPrecached ()
            throws KustvaktException, IOException, QueryException {
        int numOfMatches = testSearchWithoutRef_VC1();
        vcLoader.loadVCToCache("named-vc1", "/vc/named-vc1.jsonld");
        assertTrue(VirtualCorpusCache.contains("named-vc1"));
        JsonNode node = testSearchWithRef_VC1();
        assertEquals(numOfMatches, node.at("/matches").size());
        testStatisticsWithRef();
        numOfMatches = testSearchWithoutRef_VC2();
        vcLoader.loadVCToCache("named-vc2", "/vc/named-vc2.jsonld");
        assertTrue(VirtualCorpusCache.contains("named-vc2"));
        node = testSearchWithRef_VC2();
        assertEquals(numOfMatches, node.at("/matches").size());
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
        
        VirtualCorpusCache.delete("named-vc1");
        assertFalse(VirtualCorpusCache.contains("named-vc1"));
    }

    private int testSearchWithoutRef_VC1 () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq",
                        "textSigle=\"GOE/AGF/00000\" | textSigle=\"GOE/AGA/01784\"")
                .request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        int size = node.at("/matches").size();
        assertTrue(size > 0);
        return size;
    }

    private int testSearchWithoutRef_VC2 () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq",
                        "textSigle!=\"GOE/AGI/04846\" & textSigle!=\"GOE/AGA/01784\"")
                .request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        int size = node.at("/matches").size();
        assertTrue(size > 0);
        return size;
    }

    private JsonNode testSearchWithRef_VC1 () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system/named-vc1\"").request()
                .get();
        String ent = response.readEntity(String.class);
        return JsonUtils.readTree(ent);
    }

    private JsonNode testSearchWithRef_VC2 () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo named-vc2").request().get();
        String ent = response.readEntity(String.class);
        return JsonUtils.readTree(ent);
    }

    @Test
    public void testStatisticsWithRef () throws KustvaktException {
        String corpusQuery = "availability = /CC-BY.*/ & referTo named-vc1";
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", corpusQuery).request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(2, node.at("/documents").asInt());
        VirtualCorpusCache.delete("named-vc1");
        assertFalse(VirtualCorpusCache.contains("named-vc1"));
    }

    @Test
    public void testStatisticsWithUnknownVC () throws KustvaktException {
        String corpusQuery = "referTo unknown-vc";
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", corpusQuery).request().get();
        String ent = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(0, node.at("/documents").asInt());
        assertEquals(0, node.at("/tokens").asInt());
        assertEquals(0, node.at("/sentences").asInt());
    }
    
    @Test
    public void testRefVcNotExist () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"username/vc1\"").request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(), "username/vc1");
    }

    @Test
    public void testRefNotAuthorized () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"dory/dory-vc\"").request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(), "guest");
    }

    @Test
    public void testSearchWithRefPublishedVcGuest () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"marlin/published-vc\"").request()
                .get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
        assertEquals(node.at("/collection/operands/0/value").asText(),
                "CC-BY.*");
        assertEquals(node.at("/collection/operands/1/@type").asText(),
                "koral:doc");
        assertEquals(node.at("/collection/operands/1/value").asText(), "GOE");
        assertEquals(node.at("/collection/operands/1/key").asText(),
                "corpusSigle");
        node = node.at("/collection/operands/1/rewrites");
        assertEquals(3, node.size());
        assertEquals(node.at("/0/operation").asText(), "operation:deletion");
        assertEquals(node.at("/0/scope").asText(), "@type(koral:docGroupRef)");
        assertEquals(node.at("/1/operation").asText(), "operation:deletion");
        assertEquals(node.at("/1/scope").asText(), "ref(marlin/published-vc)");
        assertEquals(node.at("/2/operation").asText(), "operation:insertion");
    }

    @Test
    public void testSearchWithRefPublishedVc () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"marlin/published-vc\"").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("squirt", "pass"))
                .get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
        Form f = new Form();
        f.param("status", "HIDDEN");
        // check dory in the hidden group of the vc
        response = target().path(API_VERSION).path("admin").path("group")
                .path("list").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(3, node.at("/0/id").asInt());
        String members = node.at("/0/members").toString();
        assertTrue(members.contains("\"userId\":\"squirt\""));
    }
}
