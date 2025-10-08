package de.ids_mannheim.korap.web.controller.usergroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @author margaretha
 */
public class UserGroupControllerTest extends UserGroupTestBase {

    private String username = "UserGroupControllerTest";

    @Test
    public void testCreateGroupEmptyDescription ()
            throws KustvaktException {
        String groupName = "empty_group";
        Response response = createUserGroup(groupName, "", username);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        deleteGroupByName(groupName,username);
    }

    @Test
    public void testCreateGroupMissingDescription ()
            throws KustvaktException {
        String groupName = "missing-desc-group";
        Response response = testCreateGroupWithoutDescription(groupName);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        deleteGroupByName(groupName,username);
    }

    private Response testCreateGroupWithoutDescription (String groupName)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .put(Entity.form(new Form()));
        return response;
    }

    @Test
    public void testCreateGroupInvalidName ()
            throws KustvaktException {
        String groupName = "invalid-group-name$";
        Response response = testCreateGroupWithoutDescription(groupName);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        // assertEquals("User-group name must only contains letters, numbers, "
        // + "underscores, hypens and spaces", node.at("/errors/0/1").asText());
        assertEquals("invalid-group-name$", node.at("/errors/0/2").asText());
    }

    @Test
    public void testCreateGroupNameTooShort ()
            throws KustvaktException {
        String groupName = "a";
        Response response = testCreateGroupWithoutDescription(groupName);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals("groupName must contain at least 3 characters",
            node.at("/errors/0/1").asText());
        assertEquals("groupName", node.at("/errors/0/2").asText());
    }

    @Test
    public void testUserGroup () throws KustvaktException {
        String groupName = "new-user-group";
        String description = "This is new-user-group.";
        Response response = createUserGroup(groupName, description, username);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        // same name
        response = testCreateGroupWithoutDescription(groupName);
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        // list user group
        JsonNode node = listUserGroups(username);
        assertEquals(1, node.size());
        node = node.get(0);
        assertEquals("new-user-group", node.get("name").asText());
        assertEquals(description, node.get("description").asText());
        assertEquals(username, node.get("owner").asText());
        assertEquals(1, node.get("members").size());
        assertEquals(username, node.at("/members/0/userId").asText());
        assertEquals(5,  node.at("/members/0/privileges").size());

        testUpdateUserGroup(groupName);
        testAddMember(groupName, username, "darla");
        testDeleteGroup(groupName,username);
    }
    
    private void testUpdateUserGroup (String groupName)
            throws KustvaktException {
        String description = "Description is updated.";
        Response response = createUserGroup(groupName, description, username);
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        JsonNode node = listUserGroups(username);
        assertEquals(1, node.size());
        assertEquals(description, node.get(0).get("description").asText());
    }

   
    private void testDeleteGroup (String groupName, String username)
            throws KustvaktException {
        deleteGroupByName(groupName, username);
        JsonNode node = listUserGroups(username);
        assertEquals(0, node.size());
    }

    @Test
    public void testDeleteGroupUnauthorized ()
            throws KustvaktException {
        createMarlinGroup();
        addMember(marlinGroupName, "dory", "marlin");
        
        addAdminRole(marlinGroupName, "dory", "marlin");
        
        // dory is a group admin in marlin-group
        Response response = target().path(API_VERSION).path("group")
                .path("@marlin-group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: dory",
            node.at("/errors/0/1").asText());
        
        deleteGroupByName(marlinGroupName, "marlin");
    }

    @Test
    public void testDeleteDeletedGroup ()
            throws KustvaktException {
        createMarlinGroup();
        deleteGroupByName(marlinGroupName, "marlin");
        Response response = deleteGroupByName(marlinGroupName, "marlin");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteGroupOwner ()
            throws KustvaktException {
        createMarlinGroup();
        // delete marlin from marlin-group
        // dory is a group admin in marlin-group
        Response response = target().path(API_VERSION).path("group")
                .path("@marlin-group").path("~marlin").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.NOT_ALLOWED, node.at("/errors/0/0").asInt());
        assertEquals("Operation 'delete group owner'is not allowed.",
            node.at("/errors/0/1").asText());
        deleteGroupByName(marlinGroupName, "marlin");
    }
}
