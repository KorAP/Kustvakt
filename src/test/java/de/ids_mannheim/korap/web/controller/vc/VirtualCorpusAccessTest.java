package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class VirtualCorpusAccessTest extends VirtualCorpusTestBase {

    private String testUser = "VirtualCorpusAccessTest";

    @Test
    public void testlistAccessByNonVCAAdmin () throws KustvaktException {
        createDoryGroup();
        JsonNode node = listAccessByGroup("nemo", "dory-group");
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Unauthorized operation for user: nemo");
        deleteGroupByName(doryGroupName, "dory");
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
    public void testDeleteSharedVC () throws KustvaktException {
        createDoryGroup();

        String vcName = "new_project_vc";
        String username = "dory";
        createProjectVC(username, vcName);

        String groupName = "dory-group";
        shareVCByCreator(username, vcName, groupName);

        JsonNode node = listAccessByGroup(username, groupName);
        assertEquals(1, node.size());
//      System.out.println(node.toPrettyString());
      //        assertEquals(2, node.at("/0/queryId").asInt());
      assertEquals(node.at("/0/queryName").asText(), vcName);
      //        assertEquals(2, node.at("/0/userGroupId").asInt());
      assertEquals(node.at("/0/userGroupName").asText(), groupName);

        // delete project VC
        deleteVC(vcName, username, username);
        node = listAccessByGroup(username, groupName);
        assertEquals(0, node.size());

        deleteGroupByName(doryGroupName, "dory");
    }

    @Test
    public void testCreateDeleteAccess ()
            throws ProcessingException, KustvaktException {
        createMarlinGroup();

        String vcName = "marlin-vc";
        String groupName = "marlin-group";
        // check the vc type
        JsonNode node = retrieveVCInfo("marlin", "marlin", vcName);
        assertEquals(vcName, node.at("/name").asText());
        assertEquals(node.at("/type").asText(), "private");
        // share vc to group
        Response response = shareVCByCreator("marlin", vcName, groupName);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // check the vc type
        node = retrieveVCInfo("marlin", "marlin", vcName);
        assertEquals(node.at("/type").asText(), "project");
        // list vc access by marlin
        node = listAccessByGroup("marlin", groupName);
        assertEquals(1, node.size());

        // get access id
        node = node.get(0);
        assertEquals(5, node.at("/queryId").asInt());
        assertEquals(vcName, node.at("/queryName").asText());
        //        assertEquals(1, node.at("/userGroupId").asInt());
        assertEquals(groupName, node.at("/userGroupName").asText());
        assertEquals(1, node.at("/members").size());

        String roleId = node.at("/roleId").asText();
        // EM: TODO
        //        testShareVC_nonUniqueAccess("marlin", vcName, groupName);

        // delete unauthorized
        response = deleteAccess(testUser, roleId);
        testResponseUnauthorized(response, testUser);

        testDeleteAccessByAdmin(roleId, groupName);

        // edit VC back to private
        String json = "{\"type\": \"" + ResourceType.PRIVATE + "\"}";
        editVC("marlin", "marlin", vcName, json);
        node = retrieveVCInfo("marlin", "marlin", vcName);
        assertEquals(ResourceType.PRIVATE.displayName(),
                node.at("/type").asText());

        deleteGroupByName(marlinGroupName, "marlin");
    }

    private void testDeleteAccessByAdmin (String roleId, String groupName)
            throws KustvaktException {
        inviteMember(marlinGroupName, "marlin", "nemo");
        subscribe(marlinGroupName, "nemo");

        Form form = new Form();
        form.param("memberUsername", "nemo");
        form.param("role", PredefinedRole.GROUP_ADMIN.name());
        addMemberRole(marlinGroupName, "marlin", form);

        Response response = deleteAccess("nemo", roleId);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = listAccessByGroup("nemo", groupName);
        assertEquals(0, node.size());
    }

    private void testShareVC_nonUniqueAccess (String vcCreator, String vcName,
            String groupName) throws ProcessingException, KustvaktException {
        Response response = shareVCByCreator(vcCreator, vcName, groupName);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        assertEquals(StatusCodes.DB_INSERT_FAILED,
                node.at("/errors/0/0").asInt());
        // EM: message differs depending on the database used
        // for testing. The message below is from sqlite.
        // assertTrue(node.at("/errors/0/1").asText()
        // .startsWith("[SQLITE_CONSTRAINT_UNIQUE]"));
    }

    @Test
    public void testDeleteNonExistingAccess ()
            throws ProcessingException, KustvaktException {
        Response response = deleteAccess("dory", "100");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
    }
}
