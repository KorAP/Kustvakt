package de.ids_mannheim.korap.web.controller.usergroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.dao.UserGroupMemberDao;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class UserGroupMemberTest extends UserGroupTestBase {

    @Autowired
    private UserGroupMemberDao memberDao;

    @Test
    public void testAddMultipleMembers ()
            throws KustvaktException {
        createDoryGroup();
        addMember(doryGroupName, "nemo,marlin,pearl", "dory");
        
        JsonNode node = listUserGroups("dory");
        node = node.get(0);
        assertEquals(4, node.get("members").size());
        
        testAddExistingMember();
        
        deleteGroupByName(doryGroupName, "dory");
        
        testAddMemberToDeletedGroup();
    }
    
    private void testAddExistingMember () throws KustvaktException {
        Response response = addMember(doryGroupName, "nemo", "dory");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_MEMBER_EXISTS,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Username: nemo exists in the user-group: "
                        + "dory-group",
                node.at("/errors/0/1").asText());
        assertEquals("[nemo, dory-group]",
            node.at("/errors/0/2").asText());
    }
    
    private void testAddMemberToDeletedGroup () throws KustvaktException {
        Response response = addMember(doryGroupName, "pearl", "dory");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Group dory-group is not found",
                node.at("/errors/0/1").asText());
    }
    
    @Test
    public void testAddMemberMissingGroupName () throws KustvaktException {
        Response response = addMember("", "pearl","dory");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }


    @Test
    public void testAddMemberNonExistentGroup () throws KustvaktException {
        Response response = addMember("non-existent", "pearl","dory");
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Group non-existent is not found",
                node.at("/errors/0/1").asText());
    }
    
 // if username is not found in LDAP
    @Disabled
    @Test
    public void testMemberAddNonExistent () throws KustvaktException {
        createDoryGroup();
        
        Response response = addMember(doryGroupName, "bruce", "dory");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_MEMBER_NOT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("bruce is not found in the group",
                node.at("/errors/0/1").asText());
        
        testAddDeletedMember();
        deleteGroupByName(doryGroupName, "dory");
    }
    
    @Test
    public void testAddDeletedMember () throws KustvaktException {
        createDoryGroup();
        addMember(doryGroupName, "pearl", "dory");
        deleteMember(doryGroupName, "pearl", "dory");
        
        Response response = addMember(doryGroupName, "pearl", "dory");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = listUserGroups("pearl");
        assertEquals(1, node.size());
        
        deleteGroupByName(doryGroupName, "dory");
    }

    @Test
    public void testDeleteMemberByGroupOwner ()
            throws KustvaktException {
        createDoryGroup();
        addMember(doryGroupName, "pearl", "dory");
        addMember(doryGroupName, "marlin", "dory");

        testDeleteMemberUnauthorizedByNonMember(doryGroupName, "pearl", "nemo");
        testDeleteMemberUnauthorizedByMember(doryGroupName, "pearl", "marlin");
        deleteMember(doryGroupName, "pearl", "dory");

        // check group member
        JsonNode node = listUserGroups("dory");
        node = node.get(0);
        assertEquals(2, node.get("members").size());

        deleteGroupByName(doryGroupName, "dory");
    }
    
    private void testDeleteMemberUnauthorizedByNonMember (String groupName,
            String memberName, String deletedBy)
            throws KustvaktException {
        Response response = deleteMember(groupName, memberName, deletedBy);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: "+deletedBy,
            node.at("/errors/0/1").asText());
    }
    
    private void testDeleteMemberUnauthorizedByMember (String groupName,
            String memberName, String deletedBy) 
                    throws KustvaktException {
        Response response = deleteMember(groupName, memberName, deletedBy);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: "+deletedBy,
            node.at("/errors/0/1").asText());
    }

    @Test
    public void testDeleteMemberByGroupAdmin ()
            throws KustvaktException {
        createDoryGroup();
        addMember(doryGroupName, "pearl", "dory");
        addMember(doryGroupName, "nemo", "dory");
        addAdminRole(doryGroupName, "nemo", "dory");

        // check group member
        JsonNode node = listUserGroups("dory");
        node = node.get(0);
        assertEquals(3, node.get("members").size());
        
        Response response = deleteMember(doryGroupName, "pearl", "nemo");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        // check group member
        node = listUserGroups("dory");
        node = node.get(0);
        assertEquals(2, node.get("members").size());
        
        deleteGroupByName(doryGroupName, "dory");
    }
    
    @Test
    public void testDeleteMemberBySelf ()
            throws KustvaktException {
        createDoryGroup();
        addMember(doryGroupName, "pearl", "dory");

        // check group member
        JsonNode node = listUserGroups("dory");
        node = node.get(0);
        assertEquals(2, node.get("members").size());
        
        Response response = deleteMember(doryGroupName, "pearl", "pearl");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        // check group member
        node = listUserGroups("dory");
        node = node.get(0);
        assertEquals(1, node.get("members").size());
        
        deleteGroupByName(doryGroupName, "dory");
    }
    
    @Test
    public void testDeleteMemberDeletedGroup ()
            throws KustvaktException {
        createDoryGroup();
        addMember(doryGroupName, "pearl", "dory");
        deleteGroupByName(doryGroupName, "dory");
        
        Response response = deleteMember(doryGroupName, "pearl", "dory");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Group "+doryGroupName+" is not found",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testDeleteMemberAlreadyDeleted ()
            throws KustvaktException {
        createDoryGroup();
        addMember(doryGroupName, "pearl", "dory");
        deleteMember(doryGroupName, "pearl", "pearl");
        
        Response response = deleteMember(doryGroupName, "pearl", "pearl");
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_NOT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("pearl is not found in the group",
                node.at("/errors/0/1").asText());
        assertEquals("pearl",node.at("/errors/0/2").asText());
        
        deleteGroupByName(doryGroupName, "dory");
    }
    
    @Test
    public void testDeleteMemberMissingGroupName () throws KustvaktException {
        Response response = deleteMember("", "pearl","dory");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testDeleteMemberNonExistent () throws KustvaktException {
        createDoryGroup();
        Response response = deleteMember(doryGroupName, "pearl", "dory");
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.GROUP_MEMBER_NOT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("pearl is not found in the group",
                node.at("/errors/0/1").asText());
        assertEquals("pearl",node.at("/errors/0/2").asText());
        
        deleteGroupByName(doryGroupName, "dory");
    }
    
    @Test
    public void testDeleteMemberNonExistentGroup () throws KustvaktException {
        Response response = deleteMember("non-existent", "pearl","dory");
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Group non-existent is not found",
                node.at("/errors/0/1").asText());
    }
    
//    @Deprecated
//    @Test
//    public void testAddMutipleRoles () throws KustvaktException {
//        createDoryGroup();
//        inviteMember(doryGroupName, "dory", "marlin");
//        subscribe(doryGroupName, "marlin");
//        JsonNode marlinGroup = listUserGroups("marlin");
//        int groupId = marlinGroup.at("/0/id").asInt();
//        
//        Form form = new Form();
//        form.param("memberUsername", "marlin");
//        form.param("role", PredefinedRole.GROUP_ADMIN.name());
//        form.param("role", PredefinedRole.QUERY_ACCESS.name());
//        addMemberRole(doryGroupName, "dory", form);
//        
//        UserGroupMember member = memberDao.retrieveMemberById("marlin",
//                groupId);
//        Set<Role> roles = member.getRoles();
//        assertEquals(6, roles.size());
//        
//        deleteGroupByName(doryGroupName, "dory");
//    }
    
    @Test
    public void testAddMemberRole () throws KustvaktException {
        createMarlinGroup();
        addMember(marlinGroupName, "dory", "marlin");
        
        JsonNode marlinGroup = listUserGroups("marlin");
        int groupId = marlinGroup.at("/0/id").asInt();
        
        UserGroupMember member = memberDao.retrieveMemberById("dory", groupId);
        Set<Role> roles = member.getRoles();
        assertEquals(1, roles.size());
        
        Response response = addAdminRole(marlinGroupName, "dory", "marlin");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        member = memberDao.retrieveMemberById("dory", groupId);
        roles = member.getRoles();
        assertEquals(6, roles.size());
        
        testAddSameMemberRole(groupId);
        testDeleteMemberRole(groupId);

        deleteGroupByName(marlinGroupName, "marlin");
    }

    private void testAddSameMemberRole (int groupId)
            throws KustvaktException {
        Response response = addAdminRole(marlinGroupName, "dory", "marlin");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.GROUP_ADMIN_EXISTS,
                node.at("/errors/0/0").asInt());
        
        UserGroupMember member = memberDao.retrieveMemberById("dory", groupId);
        Set<Role> roles = member.getRoles();
        assertEquals(6, roles.size());
    }

    private void testDeleteMemberRole (int groupId)
            throws KustvaktException {
        Form form = new Form();
        form.param("memberUsername", "dory");
        form.param("role", PredefinedRole.GROUP_ADMIN.name());
        Response response = target().path(API_VERSION).path("group")
                .path("@marlin-group").path("role").path("delete").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        UserGroupMember member = memberDao.retrieveMemberById("dory", groupId);
        Set<Role> roles = member.getRoles();
        assertEquals(1, roles.size());
    }
}
