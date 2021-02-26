package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;

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
        ClientResponse response = resource().path(API_VERSION).path("group")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    @Test
    public void testListDoryGroups () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("list").path("system-admin")
                .queryParam("username", "dory")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(3, node.size());
    }

    @Test
    public void testListDoryActiveGroups () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("list").path("system-admin")
                .queryParam("username", "dory").queryParam("status", "ACTIVE")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
    }

    // same as list user-groups of the admin
    @Test
    public void testListWithoutUsername () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("group")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        assertEquals("[]", entity);
    }

    @Test
    public void testListByStatusAll () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("list").path("system-admin")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
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
        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("list").path("system-admin")
                .queryParam("status", "HIDDEN")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
        assertEquals(3, node.at("/0/id").asInt());
    }

    @Test
    public void testUserGroup () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        String groupName = "admin-test-group";

        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("@" + groupName)
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(testUser,
                                        "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .put(ClientResponse.class);

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
        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("@" + groupName).path("subscribe")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(
                                        memberUsername, "pass"))
                .post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testAddMemberRoles(groupName, memberUsername);
        testDeleteMemberRoles(groupName, memberUsername);
    }

    private void testAddMemberRoles (String groupName, String memberUsername)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.add("memberUsername", memberUsername);
        map.add("roleIds", "1"); // USER_GROUP_ADMIN
        map.add("roleIds", "2"); // USER_GROUP_MEMBER

        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("@" + groupName).path("role").path("add")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(adminUser,
                                        "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(map)
                .post(ClientResponse.class);

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
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.add("memberUsername", memberUsername);
        map.add("roleIds", "1"); // USER_GROUP_ADMIN

        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("@" + groupName).path("role").path("delete")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(adminUser,
                                        "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(map)
                .post(ClientResponse.class);

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
        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("@" + groupName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    private void testDeleteGroup (String groupName)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // delete group
        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("@" + groupName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check group
        JsonNode node = listGroup(testUser);
        assertEquals(0, node.size());
    }

    private void testDeleteMember (String groupName)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // delete marlin from group
        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("@" + groupName).path("~marlin")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

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
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("members", "marlin,nemo,darla");

        ClientResponse response = resource().path(API_VERSION).path("group")
                .path("@" + groupName).path("invite")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .entity(form).post(ClientResponse.class);

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
