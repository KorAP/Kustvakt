package de.ids_mannheim.korap.web.controller.usergroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.controller.OAuth2TestBase;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public abstract class UserGroupTestBase extends OAuth2TestBase {

    protected String doryGroupName = "dory-group";
    protected String marlinGroupName = "marlin-group";

    protected Response createUserGroup (String groupName, String description,
            String username) throws ProcessingException, KustvaktException {
        Form form = new Form();
        form.param("description", description);
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .put(Entity.form(form));
        return response;
    }

    protected Response deleteGroupByName (String groupName,String username)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();
//        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        return response;
    }

    protected JsonNode listUserGroups (String username)
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

    protected Response inviteMember (String groupName, String invitor,
            String invitee) throws KustvaktException {
        Form form = new Form();
        form.param("members", invitee);
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("invite").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(invitor, "pass"))
                .post(Entity.form(form));
//        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        return response;
    }
    
    protected void testInviteMember (String groupName, String invitor,
            String invitee)
            throws ProcessingException, KustvaktException {
        Response response = inviteMember(groupName, invitor, invitee);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // list group
        JsonNode node = listUserGroups(invitor);
        node = node.get(0);
        assertEquals(2, node.get("members").size());
        assertEquals(node.at("/members/1/userId").asText(), invitee);
        assertEquals(GroupMemberStatus.PENDING.name(),
                node.at("/members/1/status").asText());
        assertEquals(0, node.at("/members/1/roles").size());
    }

    protected Response subscribe (String groupName, String username)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@"+groupName).path("subscribe").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .post(Entity.form(new Form()));
//        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        return response;
    }
    
    protected Response unsubscribe (String groupName, String username)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("unsubscribe").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();
        return response;
//        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    protected void addMemberRole (String groupName, String addedBy,
            Form form) throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@"+groupName).path("role").path("add").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(addedBy, "pass"))
                .post(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    protected Response deleteMember (String groupName, String memberName,
            String deletedBy) throws KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("~"+memberName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(deletedBy, "pass"))
                .delete();
//        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        return response;
    }
    
    protected JsonNode createDoryGroup ()
            throws ProcessingException, KustvaktException {
        Response response = createUserGroup(doryGroupName,
                "This is dory-group.", "dory");
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    protected JsonNode createMarlinGroup ()
            throws ProcessingException, KustvaktException {
        Response response = createUserGroup(marlinGroupName,
                "This is marlin-group.", "marlin");
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

}
