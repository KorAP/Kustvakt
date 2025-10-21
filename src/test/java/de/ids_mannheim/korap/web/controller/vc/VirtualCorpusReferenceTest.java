package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.init.NamedVCLoader;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class VirtualCorpusReferenceTest extends VirtualCorpusTestBase {

    @Autowired
    private NamedVCLoader vcLoader;

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
        assertEquals(numOfMatches, node.at("/matches").size());
        testStatisticsWithRef();
        numOfMatches = testSearchWithoutRef_VC2();
        vcLoader.loadVCToCache("named-vc2", "/vc/named-vc2.jsonld");
        assertTrue(VirtualCorpusCache.contains("named-vc2"));
        node = testSearchWithRef_VC2();
        assertEquals(numOfMatches, node.at("/matches").size());
        
        testDeleteVC("named-vc1", "system", admin);
        testDeleteVC("named-vc2", "system", admin);
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

    private void testStatisticsWithRef () throws KustvaktException {
        String corpusQuery = "availability = /CC.*/ & referTo named-vc1";
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", corpusQuery).request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(2, node.at("/documents").asInt());
    }

    @Test
    public void testStatisticsWithUnknownVC () throws KustvaktException {
        String corpusQuery = "referTo unknown-vc";
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", corpusQuery).request().get();
        String ent = response.readEntity(String.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(ent);
		assertEquals(StatusCodes.NO_RESOURCE_FOUND,
				node.at("/errors/0/0").asInt());
        assertEquals("Virtual corpus system/unknown-vc is not found.", 
        		node.at("/errors/0/1").asText());
		assertEquals("system/unknown-vc", node.at("/errors/0/2").asText());
    }
    
    @Test
    public void testStatisticsWithUserUnknownVC () throws KustvaktException {
        String corpusQuery = "referTo \"nemo/unknown-vc\"";
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", corpusQuery).request().get();
        String ent = response.readEntity(String.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
				node.at("/errors/0/0").asInt());
        assertEquals("Virtual corpus nemo/unknown-vc is not found.", 
        		node.at("/errors/0/1").asText());
		assertEquals("nemo/unknown-vc", node.at("/errors/0/2").asText());
    }
    
    private void testStatisticsWithUserVC (String vcName) throws KustvaktException {
        String corpusQuery = "referTo \"marlin/"+vcName+"\"";
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", corpusQuery).request().get();
        String ent = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(11, node.at("/documents").asInt());
        assertEquals(665842, node.at("/tokens").asInt());
        assertEquals(25074, node.at("/sentences").asInt());
        assertEquals(772, node.at("/paragraphs").asInt());
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
        assertEquals("username/vc1", node.at("/errors/0/2").asText());
    }

    @Test
    public void testRefNotAuthorized () throws KustvaktException {
    	createDoryVC();
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"dory/dory-vc\"").request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("guest", node.at("/errors/0/2").asText());
        deleteVC("dory-vc", "dory", "dory");
    }

    @Test
    public void testSearchWithRefPublishedVcGuest () throws KustvaktException {
    	String vcName = "published-vc";
    	createMarlinPublishedVC();
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"marlin/"+vcName+"\"").request()
                .get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
        assertEquals("CC.*",
            node.at(CORPUS_PATH+"/operands/0/value").asText());
        assertEquals("koral:doc",
            node.at(CORPUS_PATH+"/operands/1/@type").asText());
        assertEquals("GOE", node.at(CORPUS_PATH+"/operands/1/value").asText());
        assertEquals("corpusSigle",
            node.at(CORPUS_PATH+"/operands/1/key").asText());

        node = node.at(CORPUS_PATH+"/operands/1");
        assertEquals("operation:override",
            node.at("/rewrites/0/operation").asText());
        assertEquals("koral:docGroupRef",
                node.at("/rewrites/0/original/@type").asText());
        assertEquals("marlin/published-vc",
                node.at("/rewrites/0/original/ref").asText());
        
        testStatisticsWithUserVC(vcName);
        testSearchWithRefPublishedVc(vcName);
        
        deleteVC("published-vc", "marlin", "marlin");
    }

    private void testSearchWithRefPublishedVc (String vcName) throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"marlin/" + vcName + "\"").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("squirt", "pass"))
                .get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);

		node = node.at(CORPUS_PATH+"/rewrites/0");
		assertEquals("operation:override", node.at("/operation").asText());
		assertEquals("marlin/published-vc",
				node.at("/original/ref").asText());
		assertEquals("koral:docGroupRef", node.at("/original/@type").asText());
        
        node = getHiddenGroup(vcName);
        assertEquals("system", node.at("/owner").asText());
        assertEquals(UserGroupStatus.HIDDEN.name(),
                node.at("/status").asText());
        node = node.at("/members");
        assertEquals("squirt", node.at("/0/userId").asText());
    }
}
