package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;
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
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.UserGroupJson;

public class UserGroupControllerTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;
    private String username = "UserGroupControllerTest";
    private String admin = "admin";

    private JsonNode retrieveUserGroups (String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path("group").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        return JsonUtils.readTree(entity);
    }

    // dory is a group admin in dory group
    @Test
    public void testListDoryGroups () throws KustvaktException {
        ClientResponse response = resource().path("group").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        JsonNode group = node.get(1);
        assertEquals(2, group.at("/id").asInt());
        assertEquals("dory group", group.at("/name").asText());
        assertEquals("dory", group.at("/owner").asText());
        assertEquals(3, group.at("/members").size());
    }

    // nemo is a group member in dory group
    @Test
    public void testListNemoGroups () throws KustvaktException {
        ClientResponse response = resource().path("group").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("nemo",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(2, node.at("/0/id").asInt());
        assertEquals("dory group", node.at("/0/name").asText());
        assertEquals("dory", node.at("/0/owner").asText());
        // group members are not allowed to see other members
        assertEquals(0, node.at("/0/members").size());
    }

    // marlin has 2 groups
    @Test
    public void testListMarlinGroups () throws KustvaktException {
        ClientResponse response = resource().path("group").path("list")
                .queryParam("username", "marlin")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("marlin",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
    }


    @Test
    public void testListGroupGuest () throws KustvaktException {
        ClientResponse response = resource().path("group").path("list")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //                System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: guest",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testCreateUserGroup () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        UserGroupJson json = new UserGroupJson();
        json.setName("new user group");
        json.setMembers(new String[] { "marlin", "nemo" });

        ClientResponse response = resource().path("group").path("create")
                .type(MediaType.APPLICATION_JSON)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(json)
                .post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // list user group
        response = resource().path("group").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        String entity = response.getEntity(String.class);
//        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
        node = node.get(0);
        assertEquals("new user group", node.get("name").asText());
        String groupId = node.get("id").asText();

        assertEquals(username, node.get("owner").asText());
        assertEquals(3, node.get("members").size());
        assertEquals(username, node.at("/members/0/userId").asText());
        assertEquals(GroupMemberStatus.ACTIVE.name(),
                node.at("/members/0/status").asText());
        assertEquals(PredefinedRole.USER_GROUP_ADMIN.name(),
                node.at("/members/0/roles/0").asText());
        assertEquals(PredefinedRole.VC_ACCESS_ADMIN.name(),
                node.at("/members/0/roles/1").asText());

        assertEquals("marlin", node.at("/members/1/userId").asText());
        assertEquals(GroupMemberStatus.PENDING.name(),
                node.at("/members/1/status").asText());
        assertEquals(0, node.at("/members/1/roles").size());

        testInviteMember(groupId);

        testDeleteMemberUnauthorized(groupId);
        testDeleteMember(groupId);
        testDeleteGroup(groupId);

        testSubscribeToDeletedGroup(groupId);
        testUnsubscribeToDeletedGroup(groupId);
    }


    private void testDeleteMember (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // delete marlin from group
        ClientResponse response = resource().path("group").path("member")
                .path("delete").queryParam("memberId", "marlin")
                .queryParam("groupId", groupId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        // check group member
        response = resource().path("group").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        node = node.get(0);
        assertEquals(3, node.get("members").size());
    }

    private void testDeleteMemberUnauthorized (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // nemo is a group member
        ClientResponse response = resource().path("group").path("member")
                .path("delete").queryParam("memberId", "marlin")
                .queryParam("groupId", groupId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("nemo",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: nemo",
                node.at("/errors/0/1").asText());
    }

    // EM: same as cancel invitation
    private void testDeletePendingMember () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        // dory delete pearl
        ClientResponse response = resource().path("group").path("member")
                .path("delete").queryParam("memberId", "pearl")
                // dory group
                .queryParam("groupId", "2")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check member
        JsonNode node = retrieveUserGroups("pearl");
        assertEquals(0, node.size());
    }

    @Test
    public void testDeleteDeletedMember () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("group").path("member")
                .path("delete").queryParam("memberId", "pearl")
                // dory group
                .queryParam("groupId", "2")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_DELETED,
                node.at("/errors/0/0").asInt());
        assertEquals("pearl has already been deleted from the group dory group",
                node.at("/errors/0/1").asText());
        assertEquals("[pearl, dory group]", node.at("/errors/0/2").asText());
    }

    private void testDeleteGroup (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        //delete group
        ClientResponse response = resource().path("group").path("delete")
                .queryParam("groupId", groupId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check group
        response = resource().path("group").path("list").path("system-admin")
                .queryParam("username", username)
                .queryParam("status", "DELETED")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(admin,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
        assertEquals(groupId, node.at("/0/id").asText());

        // check group members
        for (int i = 0; i < node.at("/0/members").size(); i++) {
            assertEquals(GroupMemberStatus.DELETED.name(),
                    node.at("/0/members/" + i + "/status").asText());
        }
    }

    @Test
    public void testDeleteGroupUnauthorized () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        // dory is a group admin in marlin group
        ClientResponse response = resource().path("group").path("delete")
                .queryParam("groupId", "1")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: dory",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testDeleteDeletedGroup () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("group").path("delete")
                .queryParam("groupId", "4")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_DELETED, node.at("/errors/0/0").asInt());
        assertEquals("Group deleted group has been deleted.",
                node.at("/errors/0/1").asText());
        assertEquals("deleted group", node.at("/errors/0/2").asText());
    }

    @Test
    public void testDeleteGroupOwner () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        // delete marlin from marlin group
        // dory is a group admin in marlin group
        ClientResponse response = resource().path("group").path("member")
                .path("delete").queryParam("memberId", "marlin")
                .queryParam("groupId", "1")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.NOT_ALLOWED, node.at("/errors/0/0").asInt());
        assertEquals("Operation 'delete group owner'is not allowed.",
                node.at("/errors/0/1").asText());
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
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .entity(userGroup).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // list group
        response = resource().path("group").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        node = node.get(0);
        assertEquals(4, node.get("members").size());

        assertEquals("darla", node.at("/members/3/userId").asText());
        assertEquals(GroupMemberStatus.PENDING.name(),
                node.at("/members/3/status").asText());
        assertEquals(0, node.at("/members/3/roles").size());
    }

    private void testInviteDeletedMember () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        String[] members = new String[] { "marlin" };

        UserGroupJson userGroup = new UserGroupJson();
        userGroup.setMembers(members);
        // dory group
        userGroup.setId(2);

        ClientResponse response = resource().path("group").path("member")
                .path("invite").type(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .entity(userGroup).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check member
        JsonNode node = retrieveUserGroups("marlin");
        assertEquals(2, node.size());
        JsonNode group = node.get(1);
        assertEquals(GroupMemberStatus.PENDING.name(),
                group.at("/userMemberStatus").asText());

    }

    @Test
    public void testInviteDeletedMember2 () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        // pearl has status deleted in dory group
        String[] members = new String[] { "pearl" };

        UserGroupJson userGroup = new UserGroupJson();
        userGroup.setMembers(members);
        // dory group
        userGroup.setId(2);

        ClientResponse response = resource().path("group").path("member")
                .path("invite").type(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .entity(userGroup).post(ClientResponse.class);

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
    public void testInvitePendingMember () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        // marlin has status PENDING in dory group
        String[] members = new String[] { "marlin" };

        UserGroupJson userGroup = new UserGroupJson();
        userGroup.setMembers(members);
        // dory group
        userGroup.setId(2);

        ClientResponse response = resource().path("group").path("member")
                .path("invite").type(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .entity(userGroup).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_EXISTS,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Username marlin with status PENDING exists in the user-group "
                        + "dory group",
                node.at("/errors/0/1").asText());
        assertEquals("[marlin, PENDING, dory group]",
                node.at("/errors/0/2").asText());
    }


    @Test
    public void testInviteActiveMember () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        // nemo has status active in dory group
        String[] members = new String[] { "nemo" };

        UserGroupJson userGroup = new UserGroupJson();
        userGroup.setMembers(members);
        // dory group
        userGroup.setId(2);

        ClientResponse response = resource().path("group").path("member")
                .path("invite").type(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .entity(userGroup).post(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_MEMBER_EXISTS,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Username nemo with status ACTIVE exists in the user-group "
                        + "dory group",
                node.at("/errors/0/1").asText());
        assertEquals("[nemo, ACTIVE, dory group]",
                node.at("/errors/0/2").asText());
    }

    @Test
    public void testInviteMemberToDeletedGroup ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        String[] members = new String[] { "nemo" };

        UserGroupJson userGroup = new UserGroupJson();
        userGroup.setMembers(members);
        // dory's deleted group
        userGroup.setId(4);

        ClientResponse response = resource().path("group").path("member")
                .path("invite").type(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .entity(userGroup).post(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_DELETED, node.at("/errors/0/0").asInt());
        assertEquals("Group deleted group has been deleted.",
                node.at("/errors/0/1").asText());
        assertEquals("deleted group", node.at("/errors/0/2").asText());
    }

    // marlin has GroupMemberStatus.PENDING in dory group
    @Test
    public void testSubscribePendingMember () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", "2");

        ClientResponse response = resource().path("group").path("subscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("marlin",
                                "pass"))
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // retrieve marlin group
        JsonNode node = retrieveUserGroups("marlin");
        // System.out.println(node);
        assertEquals(2, node.size());

        JsonNode group = node.get(1);
        assertEquals(2, group.at("/id").asInt());
        assertEquals("dory group", group.at("/name").asText());
        assertEquals("dory", group.at("/owner").asText());
        // group members are not allowed to see other members
        assertEquals(0, group.at("/members").size());
        assertEquals(GroupMemberStatus.ACTIVE.name(),
                group.at("/userMemberStatus").asText());
        assertEquals(PredefinedRole.USER_GROUP_MEMBER.name(),
                group.at("/userRoles/0").asText());
        assertEquals(PredefinedRole.VC_ACCESS_MEMBER.name(),
                group.at("/userRoles/1").asText());


        // unsubscribe marlin from dory group
        testUnsubscribeActiveMember(form);
        checkGroupMemberRole("2", "marlin");

        // invite marlin to dory group to set back the GroupMemberStatus.PENDING
        testInviteDeletedMember();
    }

    // pearl has GroupMemberStatus.DELETED in dory group
    @Test
    public void testSubscribeDeletedMember () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", "2");

        ClientResponse response = resource().path("group").path("subscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("pearl",
                                "pass"))
                .entity(form).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_DELETED,
                node.at("/errors/0/0").asInt());
        assertEquals("pearl has already been deleted from the group dory group",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testSubscribeMissingGroupId () throws KustvaktException {
        ClientResponse response = resource().path("group").path("subscribe")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, handler
                        .createBasicAuthorizationHeaderValue("bruce", "pass"))
                .post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.MISSING_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals("groupId", node.at("/errors/0/1").asText());
        assertEquals("0", node.at("/errors/0/2").asText());
    }

    @Test
    public void testSubscribeNonExistentMember () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", "2");

        ClientResponse response = resource().path("group").path("subscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("bruce",
                                "pass"))
                .entity(form).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_NOT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("bruce is not found in the group",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testSubscribeToNonExistentGroup () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", "100");

        ClientResponse response = resource().path("group").path("subscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("pearl",
                                "pass"))
                .entity(form).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_NOT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Group with id 100 is not found",
                node.at("/errors/0/1").asText());
    }

    private void testSubscribeToDeletedGroup (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", groupId);

        ClientResponse response = resource().path("group").path("subscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("nemo",
                                "pass"))
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_DELETED, node.at("/errors/0/0").asInt());
        assertEquals("Group new user group has been deleted.",
                node.at("/errors/0/1").asText());
    }

    private void testUnsubscribeActiveMember (
            MultivaluedMap<String, String> form)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path("group").path("unsubscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("marlin",
                                "pass"))
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = retrieveUserGroups("marlin");
        assertEquals(1, node.size());
    }

    private void checkGroupMemberRole (String groupId, String deletedMemberName)
            throws KustvaktException {
        ClientResponse response = resource().path("group").path(groupId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(admin,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);

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
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // pearl unsubscribes from dory group 
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        // dory group
        form.add("groupId", "2");

        ClientResponse response = resource().path("group").path("unsubscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("pearl",
                                "pass"))
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_DELETED,
                node.at("/errors/0/0").asInt());
        assertEquals("pearl has already been deleted from the group dory group",
                node.at("/errors/0/1").asText());
        assertEquals("[pearl, dory group]", node.at("/errors/0/2").asText());
    }

    @Test
    public void testUnsubscribePendingMember ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        JsonNode node = retrieveUserGroups("marlin");
        assertEquals(2, node.size());

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        // dory group
        form.add("groupId", "2");

        ClientResponse response = resource().path("group").path("unsubscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("marlin",
                                "pass"))
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        node = retrieveUserGroups("marlin");
        assertEquals(1, node.size());

        // invite marlin to dory group to set back the GroupMemberStatus.PENDING
        testInviteDeletedMember();
    }

    @Test
    public void testUnsubscribeMissingGroupId () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();

        ClientResponse response = resource().path("group").path("unsubscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("marlin",
                                "pass"))
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(StatusCodes.MISSING_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals("groupId", node.at("/errors/0/1").asText());
        assertEquals("0", node.at("/errors/0/2").asText());
    }

    @Test
    public void testUnsubscribeNonExistentMember () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", "2");

        ClientResponse response = resource().path("group").path("unsubscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("bruce",
                                "pass"))
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(StatusCodes.GROUP_MEMBER_NOT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("bruce is not found in the group",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testUnsubscribeToNonExistentGroup () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", "100");

        ClientResponse response = resource().path("group").path("unsubscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("pearl",
                                "pass"))
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(StatusCodes.GROUP_NOT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Group with id 100 is not found",
                node.at("/errors/0/1").asText());
    }

    private void testUnsubscribeToDeletedGroup (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", groupId);

        ClientResponse response = resource().path("group").path("unsubscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("nemo",
                                "pass"))
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_DELETED, node.at("/errors/0/0").asInt());
        assertEquals("Group new user group has been deleted.",
                node.at("/errors/0/1").asText());
    }
}
