package de.ids_mannheim.korap.web.controller.usergroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.controller.OAuth2TestBase;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public abstract class UserGroupTestBase extends OAuth2TestBase {

    protected String doryGroupName = "dory-group";
    protected String marlinGroupName = "marlin-group";
    protected String admin = "admin";

    protected Response createUserGroup (String groupName, String description,
            String username) throws KustvaktException {
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

    
    protected Response addMember (String groupName, String memberUsername,
            String username) throws KustvaktException {
        Form form = new Form();
        form.param("members", memberUsername);
        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("member").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .put(Entity.form(form));
//        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        return response;
    }
    
    protected void testAddMember (String groupName, String username,
            String memberUsername)
            throws KustvaktException {
        Response response = addMember(groupName, memberUsername, username);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        // list group
        JsonNode node = listUserGroups(username);
        node = node.get(0);
        assertEquals(2, node.get("members").size());
        assertEquals(memberUsername, node.at("/members/1/userId").asText());
        assertEquals(1, node.at("/members/1/privileges").size());
    }

    protected Response addAdminRole (String groupName, String memberName,
            String addedBy) throws KustvaktException {
        Form form = new Form();
        form.param("member", memberName);
        form.param("role", PredefinedRole.GROUP_ADMIN.name());

        Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName).path("role").path("add").path("admin")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(addedBy, "pass"))
                .post(Entity.form(form));
        return response;
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
    
    protected JsonNode createDoryGroup () throws KustvaktException {
        Response response = createUserGroup(doryGroupName,
                "This is dory-group.", "dory");
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    protected JsonNode createMarlinGroup () throws KustvaktException {
        Response response = createUserGroup(marlinGroupName,
                "This is marlin-group.", "marlin");
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }
    
    protected JsonNode getHiddenGroup (String queryName)
            throws KustvaktException {
        Form f = new Form();
        f.param("queryName", queryName);
        Response response = target().path(API_VERSION).path("admin")
                .path("group").path("hidden").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(f));
//        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }
    
    protected JsonNode listHiddenGroup () throws KustvaktException {
        Form f = new Form();
        f.param("status", "HIDDEN");
        Response response = target().path(API_VERSION).path("admin")
                .path("group").path("list")
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(
                                        "admin", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

}
