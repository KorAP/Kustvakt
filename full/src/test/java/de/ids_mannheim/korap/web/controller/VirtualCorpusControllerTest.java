package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.AuthenticationScheme;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 *
 */
public class VirtualCorpusControllerTest extends VirtualCorpusTestBase {

    private String testUser = "vcControllerTest";

    private String authHeader;
    
    public VirtualCorpusControllerTest () throws KustvaktException {
        authHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(testUser, "pass");
    }

    @Test
    public void testCreatePrivateVC () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";

        createVC(authHeader,testUser, "new_vc", json);

        // list user VC
        JsonNode node = listVC(testUser);
        assertEquals(2, node.size());
        assertEquals("new_vc", node.get(1).get("name").asText());

        // delete new VC
        deleteVC("new_vc", testUser, testUser);

        // list VC
        node = listVC(testUser);
        assertEquals(1, node.size());
    }

    @Test
    public void testCreatePublishedVC () throws KustvaktException {
        String json = "{\"type\": \"PUBLISHED\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";

        String vcName = "new-published-vc";
        createVC(authHeader, testUser, vcName, json);
        
        // test list owner vc
        JsonNode node = retrieveVCInfo(testUser, testUser, vcName);
        assertEquals(vcName, node.get("name").asText());

        // EM: check hidden access
        node = listAccessByGroup("admin", "");
        node = node.get(node.size() - 1);
        assertEquals("system", node.at("/createdBy").asText());
        assertEquals(vcName, node.at("/queryName").asText());
        assertTrue(node.at("/userGroupName").asText().startsWith("auto"));
        assertEquals(vcName, node.at("/queryName").asText());

        String groupName = node.at("/userGroupName").asText();

        // EM: check if hidden group has been created
        node = testCheckHiddenGroup(groupName);
        assertEquals("HIDDEN", node.at("/status").asText());

        // EM: delete vc
        deleteVC(vcName, testUser, testUser);

        // EM: check if the hidden groups are deleted as well
        node = testCheckHiddenGroup(groupName);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Group " + groupName + " is not found",
                node.at("/errors/0/1").asText());
    }

    private JsonNode testCheckHiddenGroup (String groupName)
            throws ProcessingException,
            KustvaktException {
        Response response = target().path(API_VERSION)
                .path("admin").path("group").path("@"+groupName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .post(null);

        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    @Test
    public void testCreateVCWithInvalidToken ()
            throws IOException, KustvaktException {
        String json = "{\"type\": \"PRIVATE\","
                + "\"corpusQuery\": \"corpusSigle=GOE\"}";

        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("test-invalid-signature.token");

        String authToken;
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(is));) {
            authToken = reader.readLine();
        }

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION,
                        AuthenticationScheme.BEARER.displayName() + " "
                                + authToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        String entity = response.readEntity(String.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is invalid",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateVCWithExpiredToken ()
            throws IOException, KustvaktException {
        String json = "{\"type\": \"PRIVATE\","
                + "\"corpusQuery\": \"corpusSigle=GOE\"}";

        String authToken = "fia0123ikBWn931470H8s5gRqx7Moc4p";

        Response response = target().path(API_VERSION).path("vc")
                .path("~marlin").path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION,
                        AuthenticationScheme.BEARER.displayName() + " "
                                + authToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        String entity = response.readEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.EXPIRED, node.at("/errors/0/0").asInt());
        assertEquals("Access token is expired",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateSystemVC () throws KustvaktException {
        String json = "{\"type\": \"SYSTEM\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"pubDate since 1820\"}";

        String vcName = "new_system_vc";
        Response response = target().path(API_VERSION).path("vc")
                .path("~system").path(vcName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        
        JsonNode node = listSystemVC("pearl");
        assertEquals(2, node.size());
        assertEquals(ResourceType.SYSTEM.displayName(),
                node.at("/0/type").asText());
        assertEquals(ResourceType.SYSTEM.displayName(),
                node.at("/1/type").asText());
        
        deleteVC(vcName, "system","admin");
        
        node = listSystemVC("pearl");
        assertEquals(1, node.size());
    }        
    
    @Test
    public void testCreateSystemVC_unauthorized () throws KustvaktException {
        String json = "{\"type\": \"SYSTEM\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";
        
        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        testResponseUnauthorized(response, testUser);
    }

    @Test
    public void testCreateVC_invalidName () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new $vc")
                .request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
    }

    
    @Test
    public void testCreateVC_nameTooShort () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("ne")
                .request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals("queryName must contain at least 3 characters",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testCreateVC_unauthorized () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\","
                + "\"corpusQuery\": \"creationDate since 1820\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: guest",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateVC_withoutCorpusQuery () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + "}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals("corpusQuery is null", node.at("/errors/0/1").asText());
        assertEquals("corpusQuery", node.at("/errors/0/2").asText());
    }

    @Test
    public void testCreateVC_withoutEntity () throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(""));
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals("request entity is null", node.at("/errors/0/1").asText());
        assertEquals("request entity", node.at("/errors/0/2").asText());
    }

    @Test
    public void testCreateVC_withoutType () throws KustvaktException {
        String json = "{\"corpusQuery\": " + "\"creationDate since 1820\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + "}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals("type is null", node.at("/errors/0/1").asText());
        assertEquals("type", node.at("/errors/0/2").asText());
    }

    @Test
    public void testCreateVC_withWrongType () throws KustvaktException {
        String json = "{\"type\": \"PRIVAT\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertTrue(node.at("/errors/0/1").asText().startsWith(
                "Cannot deserialize value of type `de.ids_mannheim.korap.constant."
                        + "ResourceType` from String \"PRIVAT\""));
    }
    
    @Test
    public void testMaxNumberOfVC () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";

        for (int i=1; i<6; i++) {
            createVC(authHeader,testUser, "new_vc_"+i, json);
        }

        Response response = target().path(API_VERSION).path("vc")
                .path("~" + testUser).path("new_vc_6").request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals(StatusCodes.NOT_ALLOWED,
                node.at("/errors/0/0").asInt());
        assertEquals("Cannot create virtual corpus. The maximum number of "
                + "virtual corpus has been reached.",
                node.at("/errors/0/1").asText());

        // list user VC
        node = listVC(testUser);
        assertEquals(6, node.size()); // including 1 system-vc

        // delete new VC
        for (int i=1; i<6; i++) {
            deleteVC("new_vc_"+i, testUser, testUser);
        }

        // list VC
        node = listVC(testUser);
        assertEquals(1, node.size()); // system-vc
    }

    @Test
    public void testDeleteVC_unauthorized () throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path("dory-vc")
                .request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .delete();

        testResponseUnauthorized(response, testUser);    
    }

    @Test
    public void testEditVC () throws KustvaktException {
        // 1st edit
        String json = "{\"description\": \"edited vc\"}";
        editVC("dory", "dory", "dory-vc", json);
        
        // check VC
        JsonNode node = retrieveVCInfo("dory", "dory", "dory-vc");
        assertEquals("edited vc", node.at("/description").asText());

        // 2nd edit
        json = "{\"description\": \"test vc\"}";
        editVC("dory", "dory", "dory-vc", json);
        
        // check VC
        node = retrieveVCInfo("dory", "dory", "dory-vc");
        assertEquals("test vc", node.at("/description").asText());
    }
    
    @Test
    public void testEditVCName () throws KustvaktException {
        String json = "{\"name\": \"new-name\"}";
        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path("dory-vc").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testEditCorpusQuery () throws
            ProcessingException, KustvaktException {
        JsonNode node = testRetrieveKoralQuery("dory", "dory-vc");
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and", node.at("/collection/operation").asText());
        assertEquals(2, node.at("/collection/operands").size());
        
        String json = "{\"corpusQuery\": \"corpusSigle=WPD17\"}";
        editVC("dory", "dory", "dory-vc", json);

        node = testRetrieveKoralQuery("dory", "dory-vc");
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("corpusSigle", node.at("/collection/key").asText());
        assertEquals("WPD17", node.at("/collection/value").asText());
    }
    
    private JsonNode testRetrieveKoralQuery (String username, String vcName)
            throws ProcessingException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("koralQuery").path("~" + username).path(vcName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .get();
        
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    @Test
    public void testEditVC_notOwner () throws KustvaktException {
        String json = "{\"description\": \"edited vc\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path("dory-vc")
                .request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        String entity = response.readEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: " + testUser,
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testPublishProjectVC () throws KustvaktException {

        String vcName = "group-vc";

        // check the vc type
        JsonNode node = retrieveVCInfo("dory", "dory", vcName);
        assertEquals(ResourceType.PROJECT.displayName(),
                node.get("type").asText());

        // edit vc
        String json = "{\"type\": \"PUBLISHED\"}";
        editVC("dory", "dory", vcName, json);

        // check VC
        node = testListOwnerVC("dory");
        JsonNode n = node.get(1);
        assertEquals(ResourceType.PUBLISHED.displayName(),
                n.get("type").asText());

        // check hidden VC access
        node = listAccessByGroup("admin", "");
        assertEquals(4, node.size());
        node = node.get(node.size() - 1);
        assertEquals(vcName, node.at("/queryName").asText());
        assertEquals("system", node.at("/createdBy").asText());
        assertTrue(node.at("/userGroupName").asText().startsWith("auto"));

        // edit 2nd
        json = "{\"type\": \"PROJECT\"}";
        editVC("dory", "dory", vcName, json);

        node = testListOwnerVC("dory");
        assertEquals(ResourceType.PROJECT.displayName(),
                node.get(1).get("type").asText());

        // check VC access
        node = listAccessByGroup("admin", "");
        assertEquals(3, node.size());
    }
}
