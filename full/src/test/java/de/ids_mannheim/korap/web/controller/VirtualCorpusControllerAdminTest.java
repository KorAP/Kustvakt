package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Form;

import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.client.Entity;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 *
 */
public class VirtualCorpusControllerAdminTest extends VirtualCorpusTestBase {

    private String admin = "admin";
    private String username = "VirtualCorpusControllerAdminTest";

    @Test
    public void testSearchPrivateVC () throws
            ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path("dory-vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(1, node.at("/id").asInt());
        assertEquals("dory-vc", node.at("/name").asText());
    }

    @Test
    public void testSearchProjectVC () throws
            ProcessingException, KustvaktException {

        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path("group-vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();
        String entity = response.readEntity(String.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("group-vc", node.at("/name").asText());
        assertEquals(ResourceType.PROJECT.displayName(),
                node.at("/type").asText());
    }

    @Test
    public void testListDoryVC () throws
            ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .queryParam("username", "dory")
                .request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(4, node.size());
    }

    private JsonNode testListSystemVC () throws
            ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("list").path("system-admin").queryParam("type", "SYSTEM")
                .queryParam("createdBy", "system")
                .request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    @Test
    public void testCreateSystemVC () throws KustvaktException {
        String json = "{\"type\": \"SYSTEM\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~system").path("new-system-vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = testListSystemVC();
        assertEquals(2, node.size());

        testDeleteSystemVC(admin, "new-system-vc");
    }

    private void testDeleteSystemVC (String vcCreator, String vcName)
            throws ProcessingException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~system").path(vcName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = testListSystemVC();
        assertEquals(1, node.size());
    }

    @Test
    public void testPrivateVC () throws
            ProcessingException, KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";

        String vcName = "new-vc";
        Response response = target().path(API_VERSION).path("vc")
                .path("~"+username).path(vcName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = testListUserVC(username);
        assertEquals(1, node.size());

        testEditPrivateVC(username, vcName);
        testDeletePrivateVC(username, vcName);
    }

    private JsonNode testListUserVC (String username)
            throws ProcessingException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("list").path("system-admin")
                .queryParam("createdBy", username)
                .request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    private void testEditPrivateVC (String vcCreator, String vcName)
            throws ProcessingException,
            KustvaktException {

        String json = "{\"description\": \"edited vc\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+vcCreator).path(vcName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        JsonNode node = testListUserVC(username);
        assertEquals("edited vc", node.at("/0/description").asText());
    }

    private void testDeletePrivateVC (String vcCreator, String vcName)
            throws ProcessingException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~"+vcCreator).path(vcName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = testListUserVC(vcCreator);
        assertEquals(0, node.size());
    }

//    @Deprecated
//    private String testlistAccessByVC (String groupName) throws KustvaktException {
//        Response response = target().path(API_VERSION).path("vc")
//                .path("access")
//                .queryParam("groupName", groupName)
//                .request()
//                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
//                        .createBasicAuthorizationHeaderValue(admin, "pass"))
//                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
//                .get();
//
//        String entity = response.readEntity(String.class);
//        JsonNode node = JsonUtils.readTree(entity);
//        assertEquals(1, node.size());
//        node = node.get(0);
//
//        assertEquals(admin, node.at("/createdBy").asText());
//        assertEquals(5, node.at("/vcId").asInt());
//        assertEquals("marlin-vc", node.at("/vcName").asText());
//        assertEquals(1, node.at("/userGroupId").asInt());
//        assertEquals("marlin group", node.at("/userGroupName").asText());
//
//        return node.at("/accessId").asText();
//    }

    private JsonNode testlistAccessByGroup (String groupName)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("access")
                .queryParam("groupName", groupName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
        return node.get(node.size()-1);
    }

    @Test
    public void testVCSharing () throws
            ProcessingException, KustvaktException {
        String vcCreator = "marlin";
        String vcName = "marlin-vc";
        String groupName = "marlin-group";

        testCreateVCAccess(vcCreator, vcName, groupName);
        JsonNode node = testlistAccessByGroup(groupName);

        String accessId = node.at("/accessId").asText();
        testDeleteVCAccess(accessId);

        testEditVCType(admin, vcCreator, vcName, ResourceType.PRIVATE);
    }

    private void testCreateVCAccess (String vcCreator, String vcName,
            String groupName) throws
            ProcessingException, KustvaktException {
        Response response;
        // share VC
        response = target().path(API_VERSION).path("vc").path("~"+vcCreator)
                .path(vcName).path("share").path("@"+groupName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .post(Entity.form(new Form()));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

    }

    private void testDeleteVCAccess (String accessId)
            throws ProcessingException,
            KustvaktException {

        Response response = target().path(API_VERSION).path("vc")
                .path("access").path(accessId)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

    }
}
