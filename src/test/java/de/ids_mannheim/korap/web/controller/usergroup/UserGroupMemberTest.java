package de.ids_mannheim.korap.web.controller.usergroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

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
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class UserGroupMemberTest extends UserGroupTestBase {

    @Autowired
    private UserGroupMemberDao memberDao;

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

    private void testAddSameMemberRole (int groupId)
            throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("memberUsername", "dory");
        form.param("role", PredefinedRole.USER_GROUP_MEMBER_DELETE.name());

        addMemberRole(marlinGroupName, "marlin", form);

        UserGroupMember member = memberDao.retrieveMemberById("dory", groupId);
        Set<Role> roles = member.getRoles();
        assertEquals(3, roles.size());
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
        assertEquals(2, roles.size());
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
