package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.client.Entity;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.service.UserGroupService;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author margaretha
 */
@DisplayName("User Group Controller Admin Test")
class UserGroupControllerAdminTest extends SpringJerseyTest {

    private String sysAdminUser = "admin";

    private String testUser = "group-admin";

    private JsonNode listGroup(String username) throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("group").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(testUser, "pass")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    @Test
    @DisplayName("Test List User Groups Using Admin Token")
    void testListUserGroupsUsingAdminToken() throws KustvaktException {
        Form f = new Form();
        f.param("username", "dory");
        f.param("token", "secret");
        Response response = target().path(API_VERSION).path("admin").path("group").path("list").request().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED).post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(3, node.size());
    }

    /**
     * Cannot use admin token
     * see
     * {@link UserGroupService#retrieveUserGroupByStatus(String,
     * String, de.ids_mannheim.korap.constant.UserGroupStatus)}
     *
     * @throws KustvaktException
     */
    // @Test
    // public void testListUserGroupsWithAdminToken () throws KustvaktException {
    // Response response = target().path(API_VERSION).path("group")
    // .path("list").path("system-admin")
    // .queryParam("username", "dory")
    // .queryParam("token", "secret")
    // .request()
    // .get();
    // 
    // assertEquals(Status.OK.getStatusCode(), response.getStatus());
    // String entity = response.readEntity(String.class);
    // JsonNode node = JsonUtils.readTree(entity);
    // assertEquals(3, node.size());
    // }
    @Test
    @DisplayName("Test List User Groups Unauthorized")
    void testListUserGroupsUnauthorized() throws KustvaktException {
        Form f = new Form();
        f.param("username", "dory");
        Response response = target().path(API_VERSION).path("admin").path("group").path("list").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(testUser, "pass")).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED).post(Entity.form(f));
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED, node.at("/errors/0/0").asInt());
    }

    @Test
    @DisplayName("Test List User Groups With Status")
    void testListUserGroupsWithStatus() throws KustvaktException {
        Form f = new Form();
        f.param("username", "dory");
        f.param("status", "ACTIVE");
        Response response = target().path(API_VERSION).path("admin").path("group").path("list").queryParam("username", "dory").queryParam("status", "ACTIVE").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(sysAdminUser, "pass")).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED).post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
    }

    // same as list user-groups of the admin
    @Test
    @DisplayName("Test List Without Username")
    void testListWithoutUsername() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("group").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(sysAdminUser, "pass")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        assertEquals(entity, "[]");
    }

    @Test
    @DisplayName("Test List By Status All")
    void testListByStatusAll() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("admin").path("group").path("list").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(sysAdminUser, "pass")).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED).post(null);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        boolean containsHiddenStatus = false;
        for (int i = 0; i < node.size(); i++) {
            if (node.get(i).at("/status").asText().equals("HIDDEN")) {
                containsHiddenStatus = true;
            }
        }
        assertEquals(true, containsHiddenStatus);
    }

    @Test
    @DisplayName("Test List By Status Hidden")
    void testListByStatusHidden() throws ProcessingException, KustvaktException {
        Form f = new Form();
        f.param("status", "HIDDEN");
        Response response = target().path(API_VERSION).path("admin").path("group").path("list").queryParam("status", "HIDDEN").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(sysAdminUser, "pass")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED).post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
        assertEquals(3, node.at("/0/id").asInt());
    }

    @Test
    @DisplayName("Test User Group Admin")
    void testUserGroupAdmin() throws ProcessingException, KustvaktException {
        String groupName = "admin-test-group";
        Response response = target().path(API_VERSION).path("group").path("@" + groupName).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(testUser, "password")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").put(Entity.form(new Form()));
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        // list user group
        JsonNode node = listGroup(testUser);
        assertEquals(1, node.size());
        node = node.get(0);
        assertEquals(groupName, node.get("name").asText());
        testInviteMember(groupName);
        testMemberRole("marlin", groupName);
        testDeleteMember(groupName);
        testDeleteGroup(groupName);
    }

    private void testMemberRole(String memberUsername, String groupName) throws ProcessingException, KustvaktException {
        // accept invitation
        Response response = target().path(API_VERSION).path("group").path("@" + groupName).path("subscribe").request().header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(memberUsername, "pass")).post(Entity.form(new Form()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        testAddMemberRoles(groupName, memberUsername);
        testDeleteMemberRoles(groupName, memberUsername);
    }

    private void testAddMemberRoles(String groupName, String memberUsername) throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("memberUsername", memberUsername);
        // USER_GROUP_ADMIN
        form.param("roleId", "1");
        // USER_GROUP_MEMBER
        form.param("roleId", "2");
        Response response = target().path(API_VERSION).path("group").path("@" + groupName).path("role").path("add").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(sysAdminUser, "password")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = retrieveGroup(groupName).at("/members");
        JsonNode member;
        for (int i = 0; i < node.size(); i++) {
            member = node.get(i);
            if (member.at("/userId").asText().equals(memberUsername)) {
                assertEquals(3, member.at("/roles").size());
                assertEquals(PredefinedRole.USER_GROUP_ADMIN.name(), member.at("/roles/0").asText());
                break;
            }
        }
    }

    private void testDeleteMemberRoles(String groupName, String memberUsername) throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("memberUsername", memberUsername);
        // USER_GROUP_ADMIN
        form.param("roleId", "1");
        Response response = target().path(API_VERSION).path("group").path("@" + groupName).path("role").path("delete").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(sysAdminUser, "password")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = retrieveGroup(groupName).at("/members");
        JsonNode member;
        for (int i = 0; i < node.size(); i++) {
            member = node.get(i);
            if (member.at("/userId").asText().equals(memberUsername)) {
                assertEquals(2, member.at("/roles").size());
                break;
            }
        }
    }

    private JsonNode retrieveGroup(String groupName) throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("admin").path("group").path("@" + groupName).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(sysAdminUser, "pass")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").post(null);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    private void testDeleteGroup(String groupName) throws ProcessingException, KustvaktException {
        // delete group
        Response response = target().path(API_VERSION).path("group").path("@" + groupName).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(sysAdminUser, "pass")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // check group
        JsonNode node = listGroup(testUser);
        assertEquals(0, node.size());
    }

    private void testDeleteMember(String groupName) throws ProcessingException, KustvaktException {
        // delete marlin from group
        Response response = target().path(API_VERSION).path("group").path("@" + groupName).path("~marlin").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(sysAdminUser, "pass")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // check group member
        JsonNode node = listGroup(testUser);
        node = node.get(0);
        assertEquals(3, node.get("members").size());
        assertEquals(node.at("/members/1/userId").asText(), "nemo");
        assertEquals(GroupMemberStatus.PENDING.name(), node.at("/members/1/status").asText());
    }

    private void testInviteMember(String groupName) throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("members", "marlin,nemo,darla");
        Response response = target().path(API_VERSION).path("group").path("@" + groupName).path("invite").request().header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(sysAdminUser, "pass")).post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // list group
        JsonNode node = listGroup(testUser);
        node = node.get(0);
        assertEquals(4, node.get("members").size());
        assertEquals(node.at("/members/3/userId").asText(), "darla");
        assertEquals(GroupMemberStatus.PENDING.name(), node.at("/members/1/status").asText());
        assertEquals(0, node.at("/members/1/roles").size());
    }
}
