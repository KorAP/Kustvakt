package de.ids_mannheim.korap.web.controller.usergroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.service.UserGroupService;
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
public class UserGroupControllerAdminTest extends UserGroupTestBase {

    private String testUser = "group-admin";

    private JsonNode listGroup (String username)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    @Test
    public void testListUserGroupsUsingAdminToken () throws KustvaktException {
        createDoryGroup();
        
        createMarlinGroup();
        inviteMember(marlinGroupName, "marlin", "dory");
        subscribe(marlinGroupName, "dory");
        
        String testGroup = "test-group"; 
        createUserGroup("test-group", "Test group to be deleted.", "marlin");
        inviteMember(testGroup, "marlin", "dory");
        subscribe(testGroup, "dory");
        deleteGroupByName("test-group", "marlin");

        
        Form f = new Form();
        f.param("username", "dory");
        f.param("token", "secret");
        Response response = target().path(API_VERSION).path("admin")
                .path("group").path("list").request()
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
        
        testListUserGroupsWithStatus();
        
        deleteGroupByName(doryGroupName, "dory");
        deleteGroupByName(marlinGroupName, "marlin");
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
    public void testListUserGroupsUnauthorized () throws KustvaktException {
        Form f = new Form();
        f.param("username", "dory");
        Response response = target().path(API_VERSION).path("admin")
                .path("group").path("list").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(f));
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    private void testListUserGroupsWithStatus () throws KustvaktException {
        Form f = new Form();
        f.param("username", "dory");
        f.param("status", "ACTIVE");
        
        Response response = target().path(API_VERSION).path("admin")
                .path("group").path("list").queryParam("username", "dory")
                .queryParam("status", "ACTIVE").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(
                                        admin, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
    }

    // same as list user-groups of the admin
    @Test
    public void testListWithoutUsername ()
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("group").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(
                                        admin, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        assertEquals(entity, "[]");
    }

    @Test
    public void testListByStatusAll ()
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("admin")
                .path("group").path("list").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(
                                        admin, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED)
                .post(null);
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
    public void testListHiddenGroups ()
            throws ProcessingException, KustvaktException {
        JsonNode node = listHiddenGroup();
        assertEquals(1, node.size());
    }

    @Test
    public void testUserGroupAdmin ()
            throws ProcessingException, KustvaktException {
        String groupName = "admin-test-group";
        Response response = createUserGroup(groupName, "test group", testUser);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        // list user group
        JsonNode node = listGroup(testUser);
        assertEquals(1, node.size());
        node = node.get(0);
        assertEquals(groupName, node.get("name").asText());
        testInviteMember(groupName);
        subscribe(groupName, "marlin");
        testAddAdminRole(groupName, "marlin");
        testDeleteMemberRoles(groupName, "marlin");
        testDeleteMember(groupName);
        
        // delete group
        deleteGroupByName(groupName, admin);
        // check group
        node = listGroup(testUser);
        assertEquals(0, node.size());
    }


    private void testAddAdminRole (String groupName, String memberUsername)
            throws ProcessingException, KustvaktException {
        Response response = addAdminRole(groupName, memberUsername, admin);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        JsonNode node = retrieveGroup(groupName).at("/members");
        JsonNode member;
        for (int i = 0; i < node.size(); i++) {
            member = node.get(i);
            if (member.at("/userId").asText().equals(memberUsername)) {
                assertEquals(6, member.at("/privileges").size());
                break;
            }
        }
    }

    private void testDeleteMemberRoles (String groupName, String memberUsername)
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("memberUsername", memberUsername);
        // USER_GROUP_ADMIN
        form.param("role", PredefinedRole.GROUP_ADMIN.name());
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("role").path("delete").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(
                                        admin, "password"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = retrieveGroup(groupName).at("/members");
        JsonNode member;
        for (int i = 0; i < node.size(); i++) {
            member = node.get(i);
            if (member.at("/userId").asText().equals(memberUsername)) {
                assertEquals(1, member.at("/privileges").size());
                break;
            }
        }
    }

    private JsonNode retrieveGroup (String groupName)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("admin")
                .path("group").path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(
                                        admin, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").post(null);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    private void testDeleteMember (String groupName)
            throws ProcessingException, KustvaktException {
        // delete marlin from group
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("~marlin").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(
                                        admin, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // check group member
        JsonNode node = listGroup(testUser);
        node = node.get(0);
        assertEquals(3, node.get("members").size());
        assertEquals(node.at("/members/1/userId").asText(), "nemo");
    }

    private void testInviteMember (String groupName)
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("members", "marlin,nemo,darla");
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("invite").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(
                                        admin, "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // list group
        JsonNode node = listGroup(testUser);
        node = node.get(0);
        assertEquals(4, node.get("members").size());
        assertEquals(node.at("/members/3/userId").asText(), "darla");
        assertEquals(0, node.at("/members/1/privileges").size());
    }
}
