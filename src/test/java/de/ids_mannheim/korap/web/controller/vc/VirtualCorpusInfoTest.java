package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class VirtualCorpusInfoTest extends VirtualCorpusTestBase {

    private String admin = "admin";

    private String testUser = "VirtualCorpusInfoTest";

    @Test
    public void testRetrieveSystemVC ()
            throws KustvaktException {
        JsonNode node = retrieveVCInfo(testUser, "system", "system-vc");
        assertEquals(node.at("/name").asText(), "system-vc");
        assertEquals(ResourceType.SYSTEM.displayName(),
                node.at("/type").asText());
        // assertEquals("koral:doc", node.at("/koralQuery/collection/@type").asText());
        assertTrue(node.at("/query").isMissingNode());
        assertTrue(node.at("/queryLanguage").isMissingNode());
        
        testStatistics(node);
    }

    private void testStatistics (JsonNode node) {
        assertTrue(node.at("/numberOfDoc").asInt()>0);
        assertTrue(node.at("/numberOfParagraphs").asInt()>0);
        assertTrue(node.at("/numberOfSentences").asInt()>0);
        assertTrue(node.at("/numberOfTokens").asInt()>0);
    }
    
    @Test
    public void testRetrieveSystemVC_guest ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~system").path("system-vc").request().get();
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(node.at("/name").asText(), "system-vc");
        assertEquals(ResourceType.SYSTEM.displayName(),
                node.at("/type").asText());
    }

    @Test
    public void testRetrievePrivateVC ()
            throws KustvaktException {
    	createDoryVC();
        JsonNode node = retrieveVCInfo("dory", "dory", "dory-vc");
        assertEquals(node.at("/name").asText(), "dory-vc");
        assertEquals(ResourceType.PRIVATE.displayName(),
                node.at("/type").asText());
        
        testStatistics(node);
        deleteVC("dory-vc", "dory", "dory");
    }

    @Test
    public void testRetrievePrivateVC_unauthorized ()
            throws KustvaktException {
    	createDoryVC();
        Response response = target().path(API_VERSION).path("vc").path("~dory")
                .path("dory-vc").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .get();
        testResponseUnauthorized(response, testUser);
        deleteVC("dory-vc", "dory", "dory");
    }

    @Test
    public void testRetrieveProjectVC_member ()
            throws KustvaktException {
        createDoryGroup();
        addMember(doryGroupName, "nemo", "dory");
        
        createAccess("dory", "group-vc", doryGroupName, "dory");
        
        JsonNode node = retrieveVCInfo("nemo", "dory", "group-vc");
        assertEquals(node.at("/name").asText(), "group-vc");
        assertEquals(ResourceType.PROJECT.displayName(),
                node.at("/type").asText());
        
        addMember(doryGroupName, "pearl", "dory");
        
        node = retrieveVCInfo("pearl", "dory", "group-vc");
        assertEquals(node.at("/name").asText(), "group-vc");
        assertEquals(ResourceType.PROJECT.displayName(),
                node.at("/type").asText());
        
        deleteGroupByName(doryGroupName, "dory");
    }

    @Test
    public void testRetrieveProjectVC_unauthorized ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory")
                .path("group-vc").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .get();
        testResponseUnauthorized(response, testUser);
    }

    @Test
    public void testRetrieveProjectVC_nonActiveMember ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory")
                .path("group-vc").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .get();
        testResponseUnauthorized(response, "marlin");
    }

    @Test
    public void testRetrievePrivateVC_admin ()
            throws KustvaktException {
    	createDoryVC();
        Response response = target().path(API_VERSION).path("vc").path("~dory")
                .path("dory-vc").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/name").asText(), "dory-vc");
        deleteVC("dory-vc", "dory", "dory");
    }

    @Test
    public void testRetrieveProjectVC_admin ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory")
                .path("group-vc").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/name").asText(), "group-vc");
        assertEquals(ResourceType.PROJECT.displayName(),
                node.at("/type").asText());
    }
}
