package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 */
public class VirtualCorpusControllerAdminTest extends VirtualCorpusTestBase {

    private String admin = "admin";

    private String testUser = "VirtualCorpusControllerAdminTest";

    private JsonNode testAdminListVC(String username) throws ProcessingException, KustvaktException {
        Form f = new Form();
        f.param("createdBy", username);
        Response response = target().path(API_VERSION).path("admin").path("vc").path("list").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED).post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    private JsonNode testAdminListVC_UsingAdminToken(String username, ResourceType type) throws ProcessingException, KustvaktException {
        Form f = new Form();
        f.param("createdBy", username);
        f.param("type", type.toString());
        f.param("token", "secret");
        Response response = target().path(API_VERSION).path("admin").path("vc").path("list").request().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED).post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    @Test
    public void testCreateSystemVC() throws KustvaktException {
        String json = "{\"type\": \"SYSTEM\"" + ",\"queryType\": \"VIRTUAL_CORPUS\"" + ",\"corpusQuery\": \"creationDate since 1820\"}";
        Response response = target().path(API_VERSION).path("vc").path("~system").path("new-system-vc").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON).put(Entity.json(json));
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        JsonNode node = testAdminListVC_UsingAdminToken("system", ResourceType.SYSTEM);
        assertEquals(2, node.size());
        testDeleteSystemVC(admin, "new-system-vc");
    }

    private void testDeleteSystemVC(String vcCreator, String vcName) throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~system").path(vcName).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = testAdminListVC_UsingAdminToken("system", ResourceType.SYSTEM);
        assertEquals(1, node.size());
    }

    @Test
    public void testCreatePrivateVC() throws ProcessingException, KustvaktException {
        String json = "{\"type\": \"PRIVATE\"" + ",\"queryType\": \"VIRTUAL_CORPUS\"" + ",\"corpusQuery\": \"corpusSigle=GOE\"}";
        String vcName = "new-vc";
        Response response = target().path(API_VERSION).path("vc").path("~" + testUser).path(vcName).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON).put(Entity.json(json));
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        JsonNode node = testAdminListVC(testUser);
        assertEquals(1, node.size());
        testEditPrivateVC(testUser, vcName);
        testDeletePrivateVC(testUser, vcName);
    }

    private void testEditPrivateVC(String vcCreator, String vcName) throws ProcessingException, KustvaktException {
        String json = "{\"description\": \"edited vc\"}";
        Response response = target().path(API_VERSION).path("vc").path("~" + vcCreator).path(vcName).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON).put(Entity.json(json));
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        JsonNode node = testAdminListVC(testUser);
        assertEquals(node.at("/0/description").asText(), "edited vc");
    }

    private void testDeletePrivateVC(String vcCreator, String vcName) throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~" + vcCreator).path(vcName).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = testAdminListVC(vcCreator);
        assertEquals(0, node.size());
    }

    // @Deprecated
    // private String testlistAccessByVC (String groupName) throws KustvaktException {
    // Response response = target().path(API_VERSION).path("vc")
    // .path("access")
    // .queryParam("groupName", groupName)
    // .request()
    // .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
    // .createBasicAuthorizationHeaderValue(admin, "pass"))
    // .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
    // .get();
    // 
    // String entity = response.readEntity(String.class);
    // JsonNode node = JsonUtils.readTree(entity);
    // assertEquals(1, node.size());
    // node = node.get(0);
    // 
    // assertEquals(admin, node.at("/createdBy").asText());
    // assertEquals(5, node.at("/vcId").asInt());
    // assertEquals("marlin-vc", node.at("/vcName").asText());
    // assertEquals(1, node.at("/userGroupId").asInt());
    // assertEquals("marlin group", node.at("/userGroupName").asText());
    // 
    // return node.at("/accessId").asText();
    // }
    private JsonNode testlistAccessByGroup(String groupName) throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("access").queryParam("groupName", groupName).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
        return node.get(node.size() - 1);
    }

    @Test
    public void testVCSharing() throws ProcessingException, KustvaktException {
        String vcCreator = "marlin";
        String vcName = "marlin-vc";
        String groupName = "marlin-group";
        JsonNode node2 = testAdminListVC_UsingAdminToken(vcCreator, ResourceType.PROJECT);
        assertEquals(0, node2.size());
        testCreateVCAccess(vcCreator, vcName, groupName);
        JsonNode node = testlistAccessByGroup(groupName);
        String accessId = node.at("/accessId").asText();
        testDeleteVCAccess(accessId);
        node2 = testAdminListVC_UsingAdminToken(vcCreator, ResourceType.PROJECT);
        assertEquals(1, node2.size());
        String json = "{\"type\": \"" + ResourceType.PRIVATE + "\"}";
        editVC(admin, vcCreator, vcName, json);
        node = retrieveVCInfo(admin, vcCreator, vcName);
        assertEquals(ResourceType.PRIVATE.displayName(), node.at("/type").asText());
        node2 = testAdminListVC_UsingAdminToken(vcCreator, ResourceType.PROJECT);
        assertEquals(0, node2.size());
    }

    private void testCreateVCAccess(String vcCreator, String vcName, String groupName) throws ProcessingException, KustvaktException {
        Response response;
        // share VC
        response = target().path(API_VERSION).path("vc").path("~" + vcCreator).path(vcName).path("share").path("@" + groupName).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).post(Entity.form(new Form()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testDeleteVCAccess(String accessId) throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("access").path(accessId).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
}
