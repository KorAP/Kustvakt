package de.ids_mannheim.korap.web.controller.usergroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.dao.UserGroupMemberDao;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class UserGroupMemberTest extends UserGroupTestBase {

    @Autowired
    private UserGroupMemberDao memberDao;

    @Test
    public void testInvitePendingMember ()
            throws ProcessingException, KustvaktException {
        createDoryGroup();
        inviteMember(doryGroupName, "dory", "marlin");
        
        // marlin has status PENDING in dory-group
        Response response = inviteMember(doryGroupName, "dory", "marlin");
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
        
        deleteGroupByName(doryGroupName, "dory");
    }

    @Test
    public void testInviteActiveMember ()
            throws ProcessingException, KustvaktException {
        createDoryGroup();
        inviteMember(doryGroupName, "dory", "nemo");
        subscribe(doryGroupName, "nemo");
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
        
        deleteGroupByName(doryGroupName, "dory");
        
        testInviteMemberToDeletedGroup();
    }

    private void testInviteMemberToDeletedGroup () throws KustvaktException {
        Response response = inviteMember(doryGroupName, "dory", "nemo");

        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
//        String entity = response.readEntity(String.class);
//        JsonNode node = JsonUtils.readTree(entity);
//        assertEquals(StatusCodes.GROUP_DELETED, node.at("/errors/0/0").asInt());
//        assertEquals(node.at("/errors/0/1").asText(),
//                "Group deleted-group has been deleted.");
//        assertEquals(node.at("/errors/0/2").asText(), "deleted-group");
    }
    
    @Test
    public void testAddMutipleRoles () throws KustvaktException {
        createDoryGroup();
        inviteMember(doryGroupName, "dory", "marlin");
        subscribe(doryGroupName, "marlin");
        JsonNode marlinGroup = listUserGroups("marlin");
        int groupId = marlinGroup.at("/0/id").asInt();
        
        Form form = new Form();
        form.param("memberUsername", "marlin");
        form.param("role", PredefinedRole.USER_GROUP_ADMIN_READ.name());
        form.param("role", PredefinedRole.USER_GROUP_ADMIN_WRITE.name());
        form.param("role", PredefinedRole.USER_GROUP_ADMIN_DELETE.name());
        addMemberRole(doryGroupName, "dory", form);
        
        UserGroupMember member = memberDao.retrieveMemberById("marlin",
                groupId);
        Set<Role> roles = member.getRoles();
        assertEquals(5, roles.size());
        
        deleteGroupByName(doryGroupName, "dory");
    }
    
    @Test
    public void testAddMemberRole () throws KustvaktException {
        createMarlinGroup();
        inviteMember(marlinGroupName, "marlin", "dory");
        subscribe(marlinGroupName, "dory");
        JsonNode marlinGroup = listUserGroups("marlin");
        int groupId = marlinGroup.at("/0/id").asInt();

        Form form = new Form();
        form.param("memberUsername", "dory");
        form.param("role", PredefinedRole.USER_GROUP_ADMIN_READ.name());
        addMemberRole(marlinGroupName, "marlin", form);

        UserGroupMember member = memberDao.retrieveMemberById("dory", groupId);
        Set<Role> roles = member.getRoles();
        assertEquals(3, roles.size());

        testAddSameMemberRole(groupId);
        testDeleteMemberRole(groupId);
        testEditMemberRoleEmpty(groupId);
        //        testEditMemberRole(groupId);

        deleteGroupByName(marlinGroupName, "marlin");
    }

    // EM: not work as expected since role is new.
    private void testAddSameMemberRole (int groupId)
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("memberUsername", "dory");
        form.param("role", PredefinedRole.USER_GROUP_MEMBER_DELETE.name());

        addMemberRole(marlinGroupName, "marlin", form);

        UserGroupMember member = memberDao.retrieveMemberById("dory", groupId);
        Set<Role> roles = member.getRoles();
        assertEquals(4, roles.size());
    }

    private void testDeleteMemberRole (int groupId)
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("memberUsername", "dory");
        form.param("role", PredefinedRole.USER_GROUP_ADMIN_READ.name());
        Response response = target().path(API_VERSION).path("group")
                .path("@marlin-group").path("role").path("delete").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        UserGroupMember member = memberDao.retrieveMemberById("dory", groupId);
        Set<Role> roles = member.getRoles();
        assertEquals(3, roles.size());
    }

    @Deprecated
    private void testEditMemberRoleEmpty (int groupId)
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("memberUsername", "dory");
        Response response = target().path(API_VERSION).path("group")
                .path("@marlin-group").path("role").path("edit").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        UserGroupMember member = memberDao.retrieveMemberById("dory", groupId);
        Set<Role> roles = member.getRoles();
        assertEquals(0, roles.size());
    }

    @Deprecated
    private void testEditMemberRole (int groupId)
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
        UserGroupMember member = memberDao.retrieveMemberById("dory", groupId);
        Set<Role> roles = member.getRoles();
        assertEquals(2, roles.size());
    }
}
