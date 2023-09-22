package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Virtual Corpus Sharing Test")
class VirtualCorpusSharingTest extends VirtualCorpusTestBase {

    private String testUser = "VirtualCorpusSharingTest";

    @Test
    @DisplayName("Test Share Unknown VC")
    void testShareUnknownVC() throws ProcessingException, KustvaktException {
        Response response = testShareVCByCreator("marlin", "non-existing-vc", "marlin group");
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND, node.at("/errors/0/0").asInt());
    }

    @Test
    @DisplayName("Test Share Unknown Group")
    void testShareUnknownGroup() throws ProcessingException, KustvaktException {
        Response response = testShareVCByCreator("marlin", "marlin-vc", "non-existing-group");
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND, node.at("/errors/0/0").asInt());
    }

    @Test
    @DisplayName("Test Share VC - not Owner")
    void testShareVC_notOwner() throws ProcessingException, KustvaktException {
        // dory is VCA in marlin group
        Response response = target().path(API_VERSION).path("vc").path("~marlin").path("marlin-vc").path("share").path("@marlin group").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("dory", "pass")).post(Entity.form(new Form()));
        testResponseUnauthorized(response, "dory");
    }

    @Test
    @DisplayName("Test Share VC - by Member")
    void testShareVC_byMember() throws ProcessingException, KustvaktException {
        // nemo is not VCA in marlin group
        Response response = target().path(API_VERSION).path("vc").path("~nemo").path("nemo-vc").path("share").path("@marlin-group").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("nemo", "pass")).post(Entity.form(new Form()));
        testResponseUnauthorized(response, "nemo");
    }

    @Test
    @DisplayName("Test Create Share Project VC")
    void testCreateShareProjectVC() throws KustvaktException {
        String json = "{\"type\": \"PROJECT\"" + ",\"queryType\": \"VIRTUAL_CORPUS\"" + ",\"corpusQuery\": \"corpusSigle=GOE\"}";
        String vcName = "new_project_vc";
        String authHeader = HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(testUser, "pass");
        createVC(authHeader, testUser, vcName, json);
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
        response = createUserGroup(testUser, groupName, "Owid users");
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        listUserGroup(testUser, groupName);
        testInviteMember(testUser, groupName, "darla");
        subscribeToGroup(memberName, groupName);
        checkMemberInGroup(memberName, testUser, groupName);
        // share vc to group
        testShareVCByCreator(testUser, vcName, groupName);
        node = listAccessByGroup(testUser, groupName);
        // search by member
        response = searchWithVCRef(memberName, testUser, vcName);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertTrue(node.at("/matches").size() > 0);
        // delete project VC
        deleteVC(vcName, testUser, testUser);
        // list VC
        node = listVC(testUser);
        assertEquals(1, node.size());
        // search by member
        response = searchWithVCRef(memberName, testUser, vcName);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.NO_RESOURCE_FOUND, node.at("/errors/0/0").asInt());
    }

    private Response createUserGroup(String username, String groupName, String description) throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("description", description);
        Response response = target().path(API_VERSION).path("group").path("@" + groupName).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).put(Entity.form(form));
        return response;
    }

    private JsonNode listUserGroup(String username, String groupName) throws KustvaktException {
        Response response = target().path(API_VERSION).path("group").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    private void testInviteMember(String username, String groupName, String memberName) throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("members", memberName);
        Response response = target().path(API_VERSION).path("group").path("@" + groupName).path("invite").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // list group
        JsonNode node = listUserGroup(username, groupName);
        node = node.get(0);
        assertEquals(2, node.get("members").size());
        assertEquals(memberName, node.at("/members/1/userId").asText());
        assertEquals(GroupMemberStatus.PENDING.name(), node.at("/members/1/status").asText());
        assertEquals(0, node.at("/members/1/roles").size());
    }

    private void subscribeToGroup(String username, String groupName) throws KustvaktException {
        Response response = target().path(API_VERSION).path("group").path("@" + groupName).path("subscribe").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).post(Entity.form(new Form()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void checkMemberInGroup(String memberName, String testUser, String groupName) throws KustvaktException {
        JsonNode node = listUserGroup(testUser, groupName).get(0);
        assertEquals(2, node.get("members").size());
        assertEquals(memberName, node.at("/members/1/userId").asText());
        assertEquals(GroupMemberStatus.ACTIVE.name(), node.at("/members/1/status").asText());
        assertEquals(PredefinedRole.VC_ACCESS_MEMBER.name(), node.at("/members/1/roles/1").asText());
        assertEquals(PredefinedRole.USER_GROUP_MEMBER.name(), node.at("/members/1/roles/0").asText());
    }

    private Response searchWithVCRef(String username, String vcCreator, String vcName) throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("cq", "referTo \"" + vcCreator + "/" + vcName + "\"").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "pass")).get();
        return response;
    }
}
