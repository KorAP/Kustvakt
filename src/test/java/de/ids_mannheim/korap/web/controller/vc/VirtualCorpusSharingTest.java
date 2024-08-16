package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class VirtualCorpusSharingTest extends VirtualCorpusTestBase {

    private String testUser = "VirtualCorpusSharingTest";

    @Test
    public void testShareUnknownVC () throws KustvaktException {
        Response response = shareVCByCreator("marlin", "non-existing-vc",
                "marlin group");
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testShareUnknownGroup () throws KustvaktException {
        Response response = shareVCByCreator("marlin", "marlin-vc",
                "non-existing-group");
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testShareVC_Unauthorized () throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~marlin").path("marlin-vc").path("share")
                .path("@marlin group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(new Form()));
        testResponseUnauthorized(response, "dory");
    }

    @Test
    public void testShareVC_ByGroupAdmin () throws KustvaktException {
        createMarlinGroup();
        inviteMember(marlinGroupName, "marlin", "nemo");
        subscribe(marlinGroupName, "nemo");

        JsonNode node = listRolesByGroup("marlin", marlinGroupName);
        assertEquals(0, node.size());

        // share by member unauthorized
        Response response = shareVCByCreator("nemo", "nemo-vc",
                marlinGroupName);
        testResponseUnauthorized(response, "nemo");

        addAdminRole(marlinGroupName, "nemo", "marlin");
        
        response = shareVCByCreator("nemo", "nemo-vc", marlinGroupName);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        testShareVC_redundant("nemo", "nemo-vc", marlinGroupName);;

        node = listRolesByGroup("marlin", marlinGroupName);
        assertEquals(1, node.size());
        deleteGroupByName(marlinGroupName, "marlin");
    }

    private void testShareVC_redundant (String vcCreator, String vcName,
            String groupName) throws KustvaktException {
        Response response = shareVCByCreator(vcCreator, vcName, groupName);
        assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        //        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        //        System.out.println(node.toPrettyString());
    }

    @Test
    public void testSharePrivateVC () throws KustvaktException {
        String vcName = "new_private_vc";
        createPrivateVC(testUser, vcName);

        String groupName = "DNB-group";
        Response response = createUserGroup(groupName, "DNB users", testUser);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode roleNodes = listRolesByGroup(testUser, groupName, false);
        assertEquals(5, roleNodes.size());

        String memberName = "darla";
        testInviteMember(groupName, testUser, memberName);
        subscribe(groupName, memberName);

        roleNodes = listRolesByGroup(testUser, groupName, false);
        assertEquals(6, roleNodes.size());

        // share vc to group
        shareVCByCreator(testUser, vcName, groupName);

        // check member roles
        JsonNode queryRoleNodes = listRolesByGroup(testUser, groupName);
        assertEquals(1, queryRoleNodes.size());

        testDeleteQueryAccessUnauthorized(testUser, vcName, groupName,
                memberName);
        testDeleteQueryAccessToGroup(testUser, groupName, vcName);

        deleteVC(vcName, testUser, testUser);
        deleteGroupByName(groupName, testUser);

        roleNodes = listRolesByGroup(testUser, groupName, false);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                roleNodes.at("/errors/0/0").asInt());
    }

    @Test
    public void testShareProjectVC () throws KustvaktException {
        String vcName = "new_project_vc";
        createProjectVC(testUser, vcName);

        // retrieve vc info
        JsonNode vcInfo = retrieveVCInfo(testUser, testUser, vcName);
        assertEquals(vcName, vcInfo.get("name").asText());

        // list user VC
        JsonNode node = listVC(testUser);
        assertEquals(2, node.size());
        assertEquals(vcName, node.get(1).get("name").asText());

        // search by non member
        Response response = searchWithVCRef("dory", testUser, vcName);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        // create user group
        String groupName = "owidGroup";
        String memberName = "darla";
        response = createUserGroup(groupName, "Owid users", testUser);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        testInviteMember(groupName, testUser, memberName);
        subscribe(groupName, memberName);
        checkMemberInGroup(memberName, testUser, groupName);

        // share vc to group
        shareVCByCreator(testUser, vcName, groupName);

        // check member roles
        node = listRolesByGroup(testUser, groupName);
        assertEquals(1, node.size());

        // search by member
        response = searchWithVCRef(memberName, testUser, vcName);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertTrue(node.at("/matches").size() > 0);
        // delete project VC
        testDeleteSharedVC(vcName, testUser, testUser, groupName);
        // list VC
        node = listVC(testUser);
        assertEquals(1, node.size());
        // search by member
        response = searchWithVCRef(memberName, testUser, vcName);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());

        deleteGroupByName(groupName, testUser);
    }

    @Test
    public void testShareMultipleVC () throws KustvaktException {
        String vc1 = "new_private_vc";
        String vc2 = "new_project_vc";
        createPrivateVC(testUser, vc1);
        createProjectVC(testUser, vc2);

        String groupName = "DNB-group";
        Response response = createUserGroup(groupName, "DNB users", testUser);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        String memberName = "darla";
        testInviteMember(groupName, testUser, memberName);
        subscribe(groupName, memberName);

        shareVC(testUser, vc1, groupName, testUser);
        shareVC(testUser, vc2, groupName, testUser);

        // list user VC
        JsonNode node = listVC(testUser);
        assertEquals(3, node.size());

        node = listVC(memberName);
        assertEquals(3, node.size());

        testDeleteQueryAccessBySystemAdmin(testUser, vc1, groupName, "admin");

        node = listVC(memberName);
        assertEquals(2, node.size());

        node = listVC(testUser);
        assertEquals(3, node.size());

        testDeleteQueryAccessByGroupAdmin(testUser, vc2, groupName, memberName);

        node = listVC(memberName);
        assertEquals(1, node.size());

        deleteVC(vc1, testUser, testUser);
        deleteVC(vc2, testUser, testUser);

        node = listVC(testUser);
        assertEquals(1, node.size());

        deleteGroupByName(groupName, testUser);
    }

    private void testDeleteQueryAccessToGroup (String username,
            String groupName, String vcName) throws KustvaktException {
        JsonNode roleNodes = listRolesByGroup(username, groupName, false);
        assertEquals(7, roleNodes.size());

        // delete group role
        Response response = deleteRoleByGroupAndQuery(username, vcName,
                groupName, username);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode queryRoleNodes = listRolesByGroup(username, groupName);
        assertEquals(0, queryRoleNodes.size());

        roleNodes = listRolesByGroup(username, groupName, false);
        assertEquals(6, roleNodes.size());

    }

    private void testDeleteQueryAccessUnauthorized (String vcCreator,
            String vcName, String groupName, String username)
            throws KustvaktException {
        Response response = deleteRoleByGroupAndQuery(vcCreator, vcName,
                groupName, username);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    private void testDeleteQueryAccessBySystemAdmin (String vcCreator,
            String vcName, String groupName, String username)
            throws KustvaktException {
        Response response = deleteRoleByGroupAndQuery(vcCreator, vcName,
                groupName, username);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testDeleteQueryAccessByGroupAdmin (String vcCreator,
            String vcName, String groupName, String memberName)
            throws KustvaktException {

        addAdminRole(groupName, memberName, vcCreator);
        Response response = deleteRoleByGroupAndQuery(vcCreator, vcName,
                groupName, memberName);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testDeleteSharedVC (String vcName, String vcCreator,
            String username, String groupName) throws KustvaktException {
        JsonNode node = listRolesByGroup(username, groupName);
        assertEquals(1, node.size());

        Response response = deleteVC(vcName, vcCreator, username);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        node = listRolesByGroup(username, groupName);
        assertEquals(0, node.size());
    }

    //    private JsonNode listUserGroup (String username, String groupName)
    //            throws KustvaktException {
    //        Response response = target().path(API_VERSION).path("group").request()
    //                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
    //                        .createBasicAuthorizationHeaderValue(username, "pass"))
    //                .get();
    //        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    //        String entity = response.readEntity(String.class);
    //        JsonNode node = JsonUtils.readTree(entity);
    //        return node;
    //    }

    private void checkMemberInGroup (String memberName, String testUser,
            String groupName) throws KustvaktException {
        JsonNode node = listUserGroups(testUser).get(0);
        assertEquals(2, node.get("members").size());
        assertEquals(memberName, node.at("/members/1/userId").asText());
        assertEquals(PrivilegeType.DELETE_MEMBER.name(),
                node.at("/members/1/privileges/0").asText());
    }

    @Test
    public void testlistRolesUnauthorized () throws KustvaktException {
        createDoryGroup();
        JsonNode node = listRolesByGroup("nemo", "dory-group");
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Unauthorized operation for user: nemo");
        deleteGroupByName(doryGroupName, "dory");
    }
}
