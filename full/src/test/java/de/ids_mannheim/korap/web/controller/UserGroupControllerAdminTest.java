package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Form;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.client.Entity;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 *
 */
public class UserGroupControllerAdminTest extends SpringJerseyTest {

    private String adminUser = "admin";
    private String testUser = "UserGroupControllerAdminTest";

    private JsonNode listGroup (String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    @Test
    public void testListDoryGroups () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("list").path("system-admin")
                .queryParam("username", "dory")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(3, node.size());
    }

    @Test
    public void testListDoryActiveGroups () throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("list").path("system-admin")
                .queryParam("username", "dory").queryParam("status", "ACTIVE")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
    }

    // same as list user-groups of the admin
    @Test
    public void testListWithoutUsername () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        assertEquals("[]", entity);
    }

    @Test
    public void testListByStatusAll () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("list").path("system-admin")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

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
    public void testListByStatusHidden () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("list").path("system-admin")
                .queryParam("status", "HIDDEN")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
        assertEquals(3, node.at("/0/id").asInt());
    }

    @Test
    public void testUserGroup () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        String groupName = "admin-test-group";

        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName)
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(testUser,
                                        "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .put(Entity.form(new Form()));

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

    private void testMemberRole (String memberUsername, String groupName)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        // accept invitation
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("subscribe")
                .request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(
                                        memberUsername, "pass"))
                .post(Entity.form(new Form()));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testAddMemberRoles(groupName, memberUsername);
        testDeleteMemberRoles(groupName, memberUsername);
    }

    private void testAddMemberRoles (String groupName, String memberUsername)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Form form = new Form();
        form.param("memberUsername", memberUsername);
        form.param("roleId", "1"); // USER_GROUP_ADMIN
        form.param("roleId", "2"); // USER_GROUP_MEMBER

        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("role").path("add")
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(adminUser,
                                        "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .post(Entity.form(form));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = retrieveGroup(groupName).at("/members");
        JsonNode member;
        for (int i = 0; i < node.size(); i++) {
            member = node.get(i);
            if (member.at("/userId").asText().equals(memberUsername)) {
                assertEquals(3, member.at("/roles").size());
                assertEquals(PredefinedRole.USER_GROUP_ADMIN.name(),
                        member.at("/roles/0").asText());
                break;
            }
        }
    }

    private void testDeleteMemberRoles (String groupName, String memberUsername)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Form form = new Form();
        form.param("memberUsername", memberUsername);
        form.param("roleId", "1"); // USER_GROUP_ADMIN

        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("role").path("delete")
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(adminUser,
                                        "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .post(Entity.form(form));

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

    private JsonNode retrieveGroup (String groupName)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    private void testDeleteGroup (String groupName)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // delete group
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check group
        JsonNode node = listGroup(testUser);
        assertEquals(0, node.size());
    }

    private void testDeleteMember (String groupName)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // delete marlin from group
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("~marlin")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check group member
        JsonNode node = listGroup(testUser);
        node = node.get(0);
        assertEquals(3, node.get("members").size());
        assertEquals("nemo", node.at("/members/1/userId").asText());
        assertEquals(GroupMemberStatus.PENDING.name(),
                node.at("/members/1/status").asText());
    }

    private void testInviteMember (String groupName)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Form form = new Form();
        form.param("members", "marlin,nemo,darla");

        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("invite")
                .request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .post(Entity.form(form));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // list group
        JsonNode node = listGroup(testUser);
        node = node.get(0);
        assertEquals(4, node.get("members").size());

        assertEquals("darla", node.at("/members/3/userId").asText());
        assertEquals(GroupMemberStatus.PENDING.name(),
                node.at("/members/1/status").asText());
        assertEquals(0, node.at("/members/1/roles").size());
    }

}
