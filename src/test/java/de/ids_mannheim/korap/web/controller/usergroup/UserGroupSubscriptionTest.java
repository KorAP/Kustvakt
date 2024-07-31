package de.ids_mannheim.korap.web.controller.usergroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class UserGroupSubscriptionTest extends UserGroupTestBase {
    
    @Test
    public void testSubscribeNonExistentMember () throws KustvaktException {
        createDoryGroup();
        
        Response response = subscribe(doryGroupName, "bruce");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_MEMBER_NOT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "bruce is not found in the group");
        
        testSubscribeDeletedMember();
        deleteGroupByName(doryGroupName, "dory");
    }
    
    // pearl has GroupMemberStatus.DELETED in dory-group
    private void testSubscribeDeletedMember () throws KustvaktException {
        inviteMember(doryGroupName, "dory", "pearl");
        // delete pending member
        deleteMember(doryGroupName, "pearl", "dory");
        
        Response response = subscribe(doryGroupName, "pearl");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_MEMBER_DELETED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "pearl has already been deleted from the group dory-group");
        
        testUnsubscribeDeletedMember();
        testInviteDeletedMember("pearl", "dory");
    }
    
    // marlin has GroupMemberStatus.PENDING in dory-group
    @Test
    public void testSubscribePendingMember () throws KustvaktException {
        createDoryGroup();
        testInviteMember(doryGroupName, "dory", "marlin");
        Response response = subscribe(doryGroupName, "marlin");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        // retrieve marlin group
        JsonNode node = listUserGroups("marlin");
        assertEquals(1, node.size());
        JsonNode group = node.get(0);
        assertEquals(group.at("/name").asText(), "dory-group");
        assertEquals(group.at("/owner").asText(), "dory");
        // group members are not allowed to see other members
        assertEquals(0, group.at("/members").size());
        assertEquals(GroupMemberStatus.ACTIVE.name(),
                group.at("/userMemberStatus").asText());
        
        assertEquals(1, group.at("/userPrivileges").size());
        
        // unsubscribe marlin from dory-group
        testUnsubscribeActiveMember("dory-group");
        checkGroupMemberRole("dory-group", "marlin");
        testInviteDeletedMember("marlin", "dory");
        
        deleteGroupByName(doryGroupName, "dory");
    }
    
    private void testInviteDeletedMember (String invitee, String invitor)
            throws ProcessingException, KustvaktException {
        
        Response response = inviteMember(doryGroupName, invitor, invitee);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // check member
        JsonNode node = listUserGroups(invitee);
        assertEquals(1, node.size());
        JsonNode group = node.get(0);
        assertEquals(GroupMemberStatus.PENDING.name(),
                group.at("/userMemberStatus").asText());
//        testDeletePendingMember();
    }
    
    private void checkGroupMemberRole (String groupName,
            String deletedMemberName) throws KustvaktException {
        Response response = target().path(API_VERSION).path("admin")
                .path("group").path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .post(null);
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity).at("/members");
        JsonNode member;
        for (int i = 0; i < node.size(); i++) {
            member = node.get(i);
            if (deletedMemberName.equals(member.at("/userId").asText())) {
                assertEquals(0, node.at("/privileges").size());
                break;
            }
        }
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
    public void testSubscribeToNonExistentGroup () throws KustvaktException {
        Response response = subscribe("non-existent", "pearl");
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Group non-existent is not found");
    }

    @Test
    public void testSubscribeToDeletedGroup ()
            throws ProcessingException, KustvaktException {
        createDoryGroup();
        // hard delete
        deleteGroupByName(doryGroupName, "dory");
        
        Response response = subscribe(doryGroupName, "nemo");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
//        String entity = response.readEntity(String.class);
//        JsonNode node = JsonUtils.readTree(entity);
//        assertEquals(StatusCodes.GROUP_DELETED, node.at("/errors/0/0").asInt());
//        assertEquals(node.at("/errors/0/1").asText(),
//                "Group new-user-group has been deleted.");
        testUnsubscribeToDeletedGroup(doryGroupName);
        
    }

    private void testUnsubscribeToDeletedGroup (String groupName)
            throws ProcessingException, KustvaktException {
        Response response = unsubscribe(doryGroupName, "nemo");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
//        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
//        String entity = response.readEntity(String.class);
//        JsonNode node = JsonUtils.readTree(entity);
//        assertEquals(StatusCodes.GROUP_DELETED, node.at("/errors/0/0").asInt());
//        assertEquals(node.at("/errors/0/1").asText(),
//                "Group new-user-group has been deleted.");
    }

    private void testUnsubscribeActiveMember (String groupName)
            throws ProcessingException, KustvaktException {
        Response response = unsubscribe(groupName, "marlin");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = listUserGroups("marlin");
        assertEquals(0, node.size());
    }
    
    @Test
    public void testUnsubscribePendingMember ()
            throws ProcessingException, KustvaktException {
        createDoryGroup();
        testInviteMember(doryGroupName, "dory", "marlin");
        JsonNode node = listUserGroups("marlin");
        assertEquals(1, node.size());

        Response response = unsubscribe(doryGroupName, "marlin");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = listUserGroups("marlin");
        assertEquals(0, node.size());
        // invite marlin to dory-group to set back the
        // GroupMemberStatus.PENDING
        testInviteDeletedMember("marlin","dory");
        deleteGroupByName(doryGroupName, "dory");
    }

    private void testUnsubscribeDeletedMember ()
            throws ProcessingException, KustvaktException {
        // pearl unsubscribes from dory-group
        Response response = unsubscribe(doryGroupName, "pearl");
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_DELETED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "pearl has already been deleted from the group dory-group");
        assertEquals(node.at("/errors/0/2").asText(), "[pearl, dory-group]");
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
        createDoryGroup();
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
        deleteGroupByName(doryGroupName, "dory");
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

}
