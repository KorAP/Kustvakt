package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

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

public class VirtualCorpusSharingTest extends VirtualCorpusTestBase {

    private String testUser = "VirtualCorpusSharingTest";

    @Test
    public void testShareUnknownVC ()
            throws ProcessingException, KustvaktException {
        Response response = testShareVCByCreator("marlin", "non-existing-vc",
                "marlin group");
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testShareUnknownGroup ()
            throws ProcessingException, KustvaktException {
        Response response = testShareVCByCreator("marlin", "marlin-vc",
                "non-existing-group");
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testShareVC_Unauthorized ()
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~marlin").path("marlin-vc").path("share")
                .path("@marlin group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(new Form()));
        testResponseUnauthorized(response, "dory");
    }

    @Test
    public void testShareVC_byMember ()
            throws ProcessingException, KustvaktException {
        createMarlinGroup();
        inviteMember(marlinGroupName, "marlin", "nemo");
        subscribe(marlinGroupName, "nemo");
        
        JsonNode node = listAccessByGroup("marlin", marlinGroupName);
        assertEquals(0, node.size());
        
        Response response = testShareVCByCreator("nemo", "nemo-vc",
                marlinGroupName);
        testResponseUnauthorized(response, "nemo");
        
        
        Form form = new Form();
        form.param("memberUsername", "nemo");
        form.param("role", PredefinedRole.GROUP_ADMIN.name());
        addMemberRole(marlinGroupName, "marlin", form);

        response = testShareVCByCreator("nemo", "nemo-vc", marlinGroupName);
        
        node = listAccessByGroup("marlin", marlinGroupName);
        assertEquals(1, node.size());
//        System.out.println(node.toPrettyString());
        deleteGroupByName(marlinGroupName, "marlin");
    }

    @Test
    public void testCreateShareProjectVC () throws KustvaktException {
        String json = "{\"type\": \"PROJECT\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";
        String vcName = "new_project_vc";
        String authHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(testUser, "pass");
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
        response = createUserGroup(groupName, "Owid users", testUser);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        listUserGroup(testUser, groupName);
        testInviteMember(groupName, testUser, "darla");
        subscribeToGroup(memberName, groupName);
        checkMemberInGroup(memberName, testUser, groupName);
        // share vc to group
        testShareVCByCreator(testUser, vcName, groupName);
        
        // check member roles
        node = listAccessByGroup(testUser, groupName);
        assertEquals(1, node.size());
//        System.out.println(node.toPrettyString());
        
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
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
    }

    private JsonNode listUserGroup (String username, String groupName)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("group").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    private void subscribeToGroup (String username, String groupName)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("subscribe").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .post(Entity.form(new Form()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void checkMemberInGroup (String memberName, String testUser,
            String groupName) throws KustvaktException {
        JsonNode node = listUserGroup(testUser, groupName).get(0);
        assertEquals(2, node.get("members").size());
        assertEquals(memberName, node.at("/members/1/userId").asText());
        assertEquals(GroupMemberStatus.ACTIVE.name(),
                node.at("/members/1/status").asText());
        assertEquals(PredefinedRole.GROUP_MEMBER.name(),
                node.at("/members/1/roles/0").asText());
    }

    private Response searchWithVCRef (String username, String vcCreator,
            String vcName) throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq",
                        "referTo \"" + vcCreator + "/" + vcName + "\"")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();
        return response;
    }
}
