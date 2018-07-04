package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
import de.ids_mannheim.korap.web.input.UserGroupJson;

/**
 * @author margaretha
 *
 */
public class UserGroupControllerAdminTest extends SpringJerseyTest {
    @Autowired
    private HttpAuthorizationHandler handler;

    private String adminUsername = "admin";
    private String testUsername = "UserGroupControllerAdminTest";

    private JsonNode listGroup (String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path("group").path("list")
                .queryParam("username", username)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                testUsername, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    @Test
    public void testListDoryGroups () throws KustvaktException {
        ClientResponse response = resource().path("group").path("list")
                .path("system-admin").queryParam("username", "dory")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                adminUsername, "pass"))
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
        ClientResponse response = resource().path("group").path("list")
                .path("system-admin").queryParam("username", "dory")
                .queryParam("status", "ACTIVE")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                adminUsername, "pass"))
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
        ClientResponse response = resource().path("group").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                adminUsername, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        assertEquals("[]", entity);
    }

    @Test
    public void testListByStatusAll () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response =
                resource().path("group").path("list").path("system-admin")
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        adminUsername, "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(4, node.size());

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
        ClientResponse response = resource().path("group").path("list")
                .path("system-admin").queryParam("status", "HIDDEN")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                adminUsername, "pass"))
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

        UserGroupJson json = new UserGroupJson();
        json.setName("admin test group");
        json.setMembers(new String[] { "marlin", "nemo" });

        ClientResponse response = resource().path("group").path("create")
                .type(MediaType.APPLICATION_JSON)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                testUsername, "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(json)
                .post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // list user group
        JsonNode node = listGroup(testUsername);
        assertEquals(1, node.size());
        node = node.get(0);
        assertEquals("admin test group", node.get("name").asText());

        String groupId = node.get("id").asText();
        testMemberRole("marlin", groupId);
        testInviteMember(groupId);
        testDeleteMember(groupId);
        testDeleteGroup(groupId);
    }

    private void testMemberRole (String memberUsername, String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        // accept invitation
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", groupId);

        ClientResponse response = resource().path("group").path("subscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("marlin",
                                "pass"))
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testAddMemberRoles(groupId, memberUsername);
        testDeleteMemberRoles(groupId, memberUsername);
    }

    private void testAddMemberRoles (String groupId, String memberUsername)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.add("groupId", groupId.toString());
        map.add("memberUsername", memberUsername);
        map.add("roleIds", "1"); // USER_GROUP_ADMIN
        map.add("roleIds", "2"); // USER_GROUP_MEMBER

        ClientResponse response =
                resource().path("group").path("member").path("role").path("add")
                        .type(MediaType.APPLICATION_FORM_URLENCODED)
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        adminUsername, "password"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .entity(map).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = retrieveGroup(groupId).at("/members");
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

    private void testDeleteMemberRoles (String groupId, String memberUsername)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.add("groupId", groupId.toString());
        map.add("memberUsername", memberUsername);
        map.add("roleIds", "1"); // USER_GROUP_ADMIN

        ClientResponse response = resource().path("group").path("member")
                .path("role").path("delete")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                adminUsername, "password"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(map)
                .post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = retrieveGroup(groupId).at("/members");
        JsonNode member;
        for (int i = 0; i < node.size(); i++) {
            member = node.get(i);
            if (member.at("/userId").asText().equals(memberUsername)) {
                assertEquals(2, member.at("/roles").size());
                break;
            }
        }
    }

    private JsonNode retrieveGroup (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path("group").path(groupId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                adminUsername, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    private void testDeleteGroup (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // delete group
        ClientResponse response =
                resource().path("group").path("delete").path(groupId)
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        adminUsername, "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check group
        JsonNode node = listGroup(testUsername);
        assertEquals(0, node.size());
    }

    private void testDeleteMember (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // delete marlin from group
        ClientResponse response = resource().path("group").path("member")
                .path("delete").path(groupId).path("marlin")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                adminUsername, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check group member
        JsonNode node = listGroup(testUsername);
        node = node.get(0);
        assertEquals(3, node.get("members").size());
        assertEquals("nemo", node.at("/members/1/userId").asText());
        assertEquals(GroupMemberStatus.PENDING.name(),
                node.at("/members/1/status").asText());
    }

    private void testInviteMember (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        String[] members = new String[] { "darla" };

        UserGroupJson userGroup = new UserGroupJson();
        userGroup.setMembers(members);
        userGroup.setId(Integer.parseInt(groupId));

        ClientResponse response = resource().path("group").path("member")
                .path("invite").type(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                adminUsername, "pass"))
                .entity(userGroup).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // list group
        JsonNode node = listGroup(testUsername);
        node = node.get(0);
        assertEquals(4, node.get("members").size());

        assertEquals("darla", node.at("/members/3/userId").asText());
        assertEquals(GroupMemberStatus.PENDING.name(),
                node.at("/members/3/status").asText());
        assertEquals(0, node.at("/members/3/roles").size());
    }

}
