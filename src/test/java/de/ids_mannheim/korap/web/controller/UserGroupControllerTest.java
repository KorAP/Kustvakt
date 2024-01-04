package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.client.Entity;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.dao.UserGroupMemberDao;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 */
public class UserGroupControllerTest extends SpringJerseyTest {

    @Autowired
    private UserGroupMemberDao memberDao;

    private String username = "UserGroupControllerTest";

    private String admin = "admin";

    private JsonNode retrieveUserGroups (String username)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        return JsonUtils.readTree(entity);
    }

    private void deleteGroupByName (String groupName) throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    // dory is a group admin in dory-group
    @Test
    public void testListDoryGroups () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        JsonNode group = node.get(1);
        assertEquals(2, group.at("/id").asInt());
        assertEquals(group.at("/name").asText(), "dory-group");
        assertEquals(group.at("/owner").asText(), "dory");
        assertEquals(3, group.at("/members").size());
    }

    // nemo is a group member in dory-group
    @Test
    public void testListNemoGroups () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("nemo", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/0/id").asInt());
        assertEquals(node.at("/0/name").asText(), "dory-group");
        assertEquals(node.at("/0/owner").asText(), "dory");
        // group members are not allowed to see other members
        assertEquals(0, node.at("/0/members").size());
    }

    // marlin has 2 groups
    @Test
    public void testListMarlinGroups () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
    }

    @Test
    public void testListGroupGuest () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Unauthorized operation for user: guest");
    }

    @Test
    public void testCreateGroupEmptyDescription ()
            throws ProcessingException, KustvaktException {
        String groupName = "empty_group";
        Response response = testCreateUserGroup(groupName, "");
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        deleteGroupByName(groupName);
    }

    @Test
    public void testCreateGroupMissingDescription ()
            throws ProcessingException, KustvaktException {
        String groupName = "missing-desc-group";
        Response response = testCreateGroupWithoutDescription(groupName);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        deleteGroupByName(groupName);
    }

    private Response testCreateUserGroup (String groupName, String description)
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("description", description);
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .put(Entity.form(form));
        return response;
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
        Response response = testCreateUserGroup(groupName, description);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        // same name
        response = testCreateGroupWithoutDescription(groupName);
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        // list user group
        JsonNode node = retrieveUserGroups(username);
        assertEquals(1, node.size());
        node = node.get(0);
        assertEquals(node.get("name").asText(), "new-user-group");
        assertEquals(description, node.get("description").asText());
        assertEquals(username, node.get("owner").asText());
        assertEquals(1, node.get("members").size());
        assertEquals(username, node.at("/members/0/userId").asText());
        assertEquals(GroupMemberStatus.ACTIVE.name(),
                node.at("/members/0/status").asText());
        assertEquals(PredefinedRole.VC_ACCESS_ADMIN.name(),
                node.at("/members/0/roles/1").asText());
        assertEquals(PredefinedRole.USER_GROUP_ADMIN.name(),
                node.at("/members/0/roles/0").asText());
        testUpdateUserGroup(groupName);
        testInviteMember(groupName);
        testDeleteMemberUnauthorized(groupName);
        testDeleteMember(groupName);
        testDeleteGroup(groupName);
        testSubscribeToDeletedGroup(groupName);
        testUnsubscribeToDeletedGroup(groupName);
    }

    private void testUpdateUserGroup (String groupName)
            throws ProcessingException, KustvaktException {
        String description = "Description is updated.";
        Response response = testCreateUserGroup(groupName, description);
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        JsonNode node = retrieveUserGroups(username);
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
        JsonNode node = retrieveUserGroups("pearl");
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
    }

    private void testInviteMember (String groupName)
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("members", "darla");
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("invite").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // list group
        response = target().path(API_VERSION).path("group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        node = node.get(0);
        assertEquals(2, node.get("members").size());
        assertEquals(node.at("/members/1/userId").asText(), "darla");
        assertEquals(GroupMemberStatus.PENDING.name(),
                node.at("/members/1/status").asText());
        assertEquals(0, node.at("/members/1/roles").size());
    }

    private void testInviteDeletedMember ()
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("members", "marlin");
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("invite").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // check member
        JsonNode node = retrieveUserGroups("marlin");
        assertEquals(2, node.size());
        JsonNode group = node.get(1);
        assertEquals(GroupMemberStatus.PENDING.name(),
                group.at("/userMemberStatus").asText());
    }

    @Test
    public void testInviteDeletedMember2 ()
            throws ProcessingException, KustvaktException {
        // pearl has status deleted in dory-group
        Form form = new Form();
        form.param("members", "pearl");
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("invite").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // check member
        JsonNode node = retrieveUserGroups("pearl");
        assertEquals(1, node.size());
        JsonNode group = node.get(0);
        assertEquals(GroupMemberStatus.PENDING.name(),
                group.at("/userMemberStatus").asText());
        testDeletePendingMember();
    }

    @Test
    public void testInvitePendingMember ()
            throws ProcessingException, KustvaktException {
        // marlin has status PENDING in dory-group
        Form form = new Form();
        form.param("members", "marlin");
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("invite").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(form));
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_EXISTS,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Username marlin with status PENDING exists in the user-group "
                        + "dory-group",
                node.at("/errors/0/1").asText());
        assertEquals(node.at("/errors/0/2").asText(),
                "[marlin, PENDING, dory-group]");
    }

    @Test
    public void testInviteActiveMember ()
            throws ProcessingException, KustvaktException {
        // nemo has status active in dory-group
        Form form = new Form();
        form.param("members", "nemo");
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("invite").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(form));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_MEMBER_EXISTS,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Username nemo with status ACTIVE exists in the user-group "
                        + "dory-group",
                node.at("/errors/0/1").asText());
        assertEquals(node.at("/errors/0/2").asText(),
                "[nemo, ACTIVE, dory-group]");
    }

    @Test
    public void testInviteMemberToDeletedGroup ()
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("members", "nemo");
        Response response = target().path(API_VERSION).path("group")
                .path("@deleted-group").path("invite").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(form));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_DELETED, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Group deleted-group has been deleted.");
        assertEquals(node.at("/errors/0/2").asText(), "deleted-group");
    }

    // marlin has GroupMemberStatus.PENDING in dory-group
    @Test
    public void testSubscribePendingMember () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("subscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .post(Entity.form(new Form()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // retrieve marlin group
        JsonNode node = retrieveUserGroups("marlin");
        // System.out.println(node);
        assertEquals(2, node.size());
        JsonNode group = node.get(1);
        assertEquals(2, group.at("/id").asInt());
        assertEquals(group.at("/name").asText(), "dory-group");
        assertEquals(group.at("/owner").asText(), "dory");
        // group members are not allowed to see other members
        assertEquals(0, group.at("/members").size());
        assertEquals(GroupMemberStatus.ACTIVE.name(),
                group.at("/userMemberStatus").asText());
        assertEquals(PredefinedRole.VC_ACCESS_MEMBER.name(),
                group.at("/userRoles/1").asText());
        assertEquals(PredefinedRole.USER_GROUP_MEMBER.name(),
                group.at("/userRoles/0").asText());
        // unsubscribe marlin from dory-group
        testUnsubscribeActiveMember("dory-group");
        checkGroupMemberRole("dory-group", "marlin");
        // invite marlin to dory-group to set back the
        // GroupMemberStatus.PENDING
        testInviteDeletedMember();
    }

    // pearl has GroupMemberStatus.DELETED in dory-group
    @Test
    public void testSubscribeDeletedMember () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("subscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("pearl", "pass"))
                .post(Entity.form(new Form()));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_DELETED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "pearl has already been deleted from the group dory-group");
    }

    @Test
    public void testSubscribeMissingGroupName () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("subscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("bruce", "pass"))
                .post(Entity.form(new Form()));
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testSubscribeNonExistentMember () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("subscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("bruce", "pass"))
                .post(Entity.form(new Form()));
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_NOT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "bruce is not found in the group");
    }

    @Test
    public void testSubscribeToNonExistentGroup () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@non-existent").path("subscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("pearl", "pass"))
                .post(Entity.form(new Form()));
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Group non-existent is not found");
    }

    private void testSubscribeToDeletedGroup (String groupName)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("subscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("nemo", "pass"))
                .post(Entity.form(new Form()));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_DELETED, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Group new-user-group has been deleted.");
    }

    private void testUnsubscribeActiveMember (String groupName)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("unsubscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = retrieveUserGroups("marlin");
        assertEquals(1, node.size());
    }

    private void checkGroupMemberRole (String groupName,
            String deletedMemberName) throws KustvaktException {
        Response response = target().path(API_VERSION).path("admin")
                .path("group").path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .post(null);
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity).at("/members");
        JsonNode member;
        for (int i = 0; i < node.size(); i++) {
            member = node.get(i);
            if (deletedMemberName.equals(member.at("/userId").asText())) {
                assertEquals(0, node.at("/roles").size());
                break;
            }
        }
    }

    @Test
    public void testUnsubscribeDeletedMember ()
            throws ProcessingException, KustvaktException {
        // pearl unsubscribes from dory-group
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("unsubscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("pearl", "pass"))
                .delete();
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

    @Test
    public void testUnsubscribePendingMember ()
            throws ProcessingException, KustvaktException {
        JsonNode node = retrieveUserGroups("marlin");
        assertEquals(2, node.size());
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("unsubscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = retrieveUserGroups("marlin");
        assertEquals(1, node.size());
        // invite marlin to dory-group to set back the
        // GroupMemberStatus.PENDING
        testInviteDeletedMember();
    }

    @Test
    public void testUnsubscribeMissingGroupName () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("unsubscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .delete();
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUnsubscribeNonExistentMember () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@dory-group").path("unsubscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("bruce", "pass"))
                .delete();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_MEMBER_NOT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "bruce is not found in the group");
    }

    @Test
    public void testUnsubscribeToNonExistentGroup () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@tralala-group").path("unsubscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("pearl", "pass"))
                .delete();
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Group tralala-group is not found");
    }

    private void testUnsubscribeToDeletedGroup (String groupName)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("unsubscribe").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("nemo", "pass"))
                .delete();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_DELETED, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Group new-user-group has been deleted.");
    }

    @Test
    public void testAddSameMemberRole ()
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("memberUsername", "dory");
        form.param("roleId", "1");
        Response response = target().path(API_VERSION).path("group")
                .path("@marlin-group").path("role").path("add").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        UserGroupMember member = memberDao.retrieveMemberById("dory", 1);
        Set<Role> roles = member.getRoles();
        assertEquals(2, roles.size());
    }

    @Test
    public void testDeleteAddMemberRole ()
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("memberUsername", "dory");
        form.param("roleId", "1");
        Response response = target().path(API_VERSION).path("group")
                .path("@marlin-group").path("role").path("delete").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        UserGroupMember member = memberDao.retrieveMemberById("dory", 1);
        Set<Role> roles = member.getRoles();
        assertEquals(1, roles.size());
        testAddSameMemberRole();
    }

    @Test
    public void testEditMemberRoleEmpty ()
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("memberUsername", "dory");
        Response response = target().path(API_VERSION).path("group")
                .path("@marlin-group").path("role").path("edit").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        UserGroupMember member = memberDao.retrieveMemberById("dory", 1);
        Set<Role> roles = member.getRoles();
        assertEquals(0, roles.size());
        testEditMemberRole();
    }

    private void testEditMemberRole ()
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("memberUsername", "dory");
        form.param("roleId", "1");
        form.param("roleId", "3");
        Response response = target().path(API_VERSION).path("group")
                .path("@marlin-group").path("role").path("edit").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        UserGroupMember member = memberDao.retrieveMemberById("dory", 1);
        Set<Role> roles = member.getRoles();
        assertEquals(2, roles.size());
    }
}
