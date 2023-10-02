package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VirtualCorpusAccessTest extends VirtualCorpusTestBase {

    private String testUser = "VirtualCorpusAccessTest";

    @Test
    public void testlistAccessByNonVCAAdmin() throws KustvaktException {
        JsonNode node = listAccessByGroup("nemo", "dory-group");
        assertEquals(StatusCodes.AUTHORIZATION_FAILED, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(), "Unauthorized operation for user: nemo");
    }

    // @Test
    // public void testlistAccessMissingId () throws KustvaktException
    // {
    // Response response =
    // target().path(API_VERSION).path("vc")
    // .path("access")
    // .request().header(Attributes.AUTHORIZATION,
    // HttpAuthorizationHandler
    // .createBasicAuthorizationHeaderValue(
    // testUser, "pass"))
    // .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
    // .get();
    // String entity = response.readEntity(String.class);
    // JsonNode node = JsonUtils.readTree(entity);
    // assertEquals(Status.BAD_REQUEST.getStatusCode(),
    // response.getStatus());
    // assertEquals(StatusCodes.MISSING_PARAMETER,
    // node.at("/errors/0/0").asInt());
    // assertEquals("vcId", node.at("/errors/0/1").asText());
    // }
    @Test
    public void testlistAccessByGroup() throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("access").queryParam("groupName", "dory-group").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("dory", "pass")).get();
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.at("/0/accessId").asInt());
        assertEquals(2, node.at("/0/queryId").asInt());
        assertEquals(node.at("/0/queryName").asText(), "group-vc");
        assertEquals(2, node.at("/0/userGroupId").asInt());
        assertEquals(node.at("/0/userGroupName").asText(), "dory-group");
    }

    @Test
    public void testDeleteSharedVC() throws KustvaktException {
        String json = "{\"type\": \"PROJECT\"" + ",\"queryType\": \"VIRTUAL_CORPUS\"" + ",\"corpusQuery\": \"corpusSigle=GOE\"}";
        String vcName = "new_project_vc";
        String username = "dory";
        String authHeader = HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass");
        createVC(authHeader, username, vcName, json);
        String groupName = "dory-group";
        testShareVCByCreator(username, vcName, groupName);
        JsonNode node = listAccessByGroup(username, groupName);
        assertEquals(2, node.size());
        // delete project VC
        deleteVC(vcName, username, username);
        node = listAccessByGroup(username, groupName);
        assertEquals(1, node.size());
    }

    @Test
    public void testCreateDeleteAccess() throws ProcessingException, KustvaktException {
        String vcName = "marlin-vc";
        String groupName = "marlin-group";
        // check the vc type
        JsonNode node = retrieveVCInfo("marlin", "marlin", vcName);
        assertEquals(vcName, node.at("/name").asText());
        assertEquals(node.at("/type").asText(), "private");
        // share vc to group
        Response response = testShareVCByCreator("marlin", vcName, groupName);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // check the vc type
        node = retrieveVCInfo("marlin", "marlin", vcName);
        assertEquals(node.at("/type").asText(), "project");
        // list vc access by marlin
        node = listAccessByGroup("marlin", groupName);
        assertEquals(2, node.size());
        // get access id
        node = node.get(1);
        assertEquals(5, node.at("/queryId").asInt());
        assertEquals(vcName, node.at("/queryName").asText());
        assertEquals(1, node.at("/userGroupId").asInt());
        assertEquals(groupName, node.at("/userGroupName").asText());
        String accessId = node.at("/accessId").asText();
        testShareVC_nonUniqueAccess("marlin", vcName, groupName);
        // delete unauthorized
        response = testDeleteAccess(testUser, accessId);
        testResponseUnauthorized(response, testUser);
        // delete access by vc-admin
        // dory is a vc-admin in marlin group
        response = testDeleteAccess("dory", accessId);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // list vc access by dory
        node = listAccessByGroup("dory", groupName);
        assertEquals(1, node.size());
        // edit VC back to private
        String json = "{\"type\": \"" + ResourceType.PRIVATE + "\"}";
        editVC("marlin", "marlin", vcName, json);
        node = retrieveVCInfo("marlin", "marlin", vcName);
        assertEquals(ResourceType.PRIVATE.displayName(), node.at("/type").asText());
    }

    private void testShareVC_nonUniqueAccess(String vcCreator, String vcName, String groupName) throws ProcessingException, KustvaktException {
        Response response = testShareVCByCreator(vcCreator, vcName, groupName);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        assertEquals(StatusCodes.DB_INSERT_FAILED, node.at("/errors/0/0").asInt());
        // EM: message differs depending on the database used
        // for testing. The message below is from sqlite.
        // assertTrue(node.at("/errors/0/1").asText()
        // .startsWith("[SQLITE_CONSTRAINT_UNIQUE]"));
    }

    private Response testDeleteAccess(String username, String accessId) throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("access").path(accessId).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).delete();
        return response;
    }

    @Test
    public void testDeleteNonExistingAccess() throws ProcessingException, KustvaktException {
        Response response = testDeleteAccess("dory", "100");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.NO_RESOURCE_FOUND, node.at("/errors/0/0").asInt());
    }
}
