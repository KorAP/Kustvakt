package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.UserGroupJson;

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
                                adminUsername, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
                System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);

        return node;
    }

    @Test
    public void testListDoryGroups () throws KustvaktException {
        JsonNode node = listGroup("dory");
        JsonNode group = node.get(1);
        assertEquals(2, group.at("/id").asInt());
        assertEquals("dory group", group.at("/name").asText());
        assertEquals("dory", group.at("/owner").asText());
        assertEquals(3, group.at("/members").size());
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
    public void testCreateUserGroup () throws UniformInterfaceException,
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
//        testInviteMember(groupId);
//        testDeleteMember(groupId);
        testDeleteGroup(groupId);
    }

    private void testDeleteGroup (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        //delete group
        ClientResponse response = resource().path("group").path("delete")
                .queryParam("groupId", groupId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                adminUsername, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check group
//        JsonNode node = listGroup(testUsername);
//        assertEquals(0, node.size());
    }

    private void testDeleteMember (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // delete marlin from group
        ClientResponse response = resource().path("group").path("member")
                .path("delete").queryParam("memberId", "marlin")
                .queryParam("groupId", groupId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                adminUsername, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

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
        assertEquals(PredefinedRole.USER_GROUP_MEMBER.name(),
                node.at("/members/3/roles/0").asText());
        assertEquals(PredefinedRole.VC_ACCESS_MEMBER.name(),
                node.at("/members/3/roles/1").asText());
    }

}
