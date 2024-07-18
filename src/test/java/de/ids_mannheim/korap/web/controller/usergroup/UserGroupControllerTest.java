package de.ids_mannheim.korap.web.controller.usergroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.dao.UserGroupMemberDao;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @author margaretha
 */
public class UserGroupControllerTest extends UserGroupTestBase {

    private String username = "UserGroupControllerTest";

    private String admin = "admin";

    
    @Test
    public void testCreateGroupEmptyDescription ()
            throws ProcessingException, KustvaktException {
        String groupName = "empty_group";
        Response response = createUserGroup(groupName, "", username);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        deleteGroupByName(groupName,username);
    }

    @Test
    public void testCreateGroupMissingDescription ()
            throws ProcessingException, KustvaktException {
        String groupName = "missing-desc-group";
        Response response = testCreateGroupWithoutDescription(groupName);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        deleteGroupByName(groupName,username);
    }

    private Response testCreateGroupWithoutDescription (String groupName)
            throws ProcessingException, KustvaktException {
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
            throws ProcessingException, KustvaktException {
        String groupName = "invalid-group-name$";
        Response response = testCreateGroupWithoutDescription(groupName);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        // assertEquals("User-group name must only contains letters, numbers, "
        // + "underscores, hypens and spaces", node.at("/errors/0/1").asText());
        assertEquals(node.at("/errors/0/2").asText(), "invalid-group-name$");
    }

    @Test
    public void testCreateGroupNameTooShort ()
            throws ProcessingException, KustvaktException {
        String groupName = "a";
        Response response = testCreateGroupWithoutDescription(groupName);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "groupName must contain at least 3 characters");
        assertEquals(node.at("/errors/0/2").asText(), "groupName");
    }

    @Test
    public void testUserGroup () throws ProcessingException, KustvaktException {
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
        assertEquals(node.get("name").asText(), "new-user-group");
        assertEquals(description, node.get("description").asText());
        assertEquals(username, node.get("owner").asText());
        assertEquals(1, node.get("members").size());
        assertEquals(username, node.at("/members/0/userId").asText());
        assertEquals(GroupMemberStatus.ACTIVE.name(),
                node.at("/members/0/status").asText());
        assertEquals(6,  node.at("/members/0/roles").size());

        testUpdateUserGroup(groupName);
        testInviteMember(groupName, username, "darla");
        testDeleteMemberUnauthorized(groupName);
        testDeleteMember(groupName);
        testDeleteGroup(groupName);
//        testSubscribeToDeletedGroup(groupName);
//        testUnsubscribeToDeletedGroup(groupName);
    }
    
    private void testUpdateUserGroup (String groupName)
            throws ProcessingException, KustvaktException {
        String description = "Description is updated.";
        Response response = createUserGroup(groupName, description, username);
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        JsonNode node = listUserGroups(username);
        assertEquals(1, node.size());
        assertEquals(description, node.get(0).get("description").asText());
    }

    private void testDeleteMember (String groupName)
            throws ProcessingException, KustvaktException {
        // delete darla from group
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("~darla").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        // check group member
        response = target().path(API_VERSION).path("group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        node = node.get(0);
        assertEquals(1, node.get("members").size());
    }

    private void testDeleteMemberUnauthorized (String groupName)
            throws ProcessingException, KustvaktException {
        // nemo is a group member
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("~darla").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("nemo", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Unauthorized operation for user: nemo");
    }

    // EM: same as cancel invitation
    private void testDeletePendingMember ()
            throws ProcessingException, KustvaktException {
        // dory delete pearl
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("~pearl").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // check member
        JsonNode node = listUserGroups("pearl");
        assertEquals(0, node.size());
    }

    @Test
    public void testDeleteDeletedMember ()
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("~pearl").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_DELETED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "pearl has already been deleted from the group dory-group");
        assertEquals(node.at("/errors/0/2").asText(), "[pearl, dory-group]");
    }

    private void testDeleteGroup (String groupName)
            throws ProcessingException, KustvaktException {
        // delete group
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Form f = new Form();
        f.param("username", username);
        f.param("status", "DELETED");
        // EM: this is so complicated because the group retrieval are not allowed
        // for delete groups
        // check group
        response = target().path(API_VERSION).path("admin").path("group")
                .path("list").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        for (int j = 0; j < node.size(); j++) {
            JsonNode group = node.get(j);
            // check group members
            for (int i = 0; i < group.at("/0/members").size(); i++) {
                assertEquals(GroupMemberStatus.DELETED.name(),
                        group.at("/0/members/" + i + "/status").asText());
            }
        }
    }

    @Test
    public void testDeleteGroupUnauthorized ()
            throws ProcessingException, KustvaktException {
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
        assertEquals(node.at("/errors/0/1").asText(),
                "Unauthorized operation for user: dory");
    }

    @Test
    public void testDeleteDeletedGroup ()
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@deleted-group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_DELETED, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Group deleted-group has been deleted.");
        assertEquals(node.at("/errors/0/2").asText(), "deleted-group");
    }

    @Test
    public void testDeleteGroupOwner ()
            throws ProcessingException, KustvaktException {
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
        assertEquals(node.at("/errors/0/1").asText(),
                "Operation 'delete group owner'is not allowed.");
        deleteGroupByName(marlinGroupName, "marlin");
    }
}
