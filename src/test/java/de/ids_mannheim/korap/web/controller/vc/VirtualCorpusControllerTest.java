package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.AuthenticationScheme;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @author margaretha
 */
public class VirtualCorpusControllerTest extends VirtualCorpusTestBase {

    private String testUser = "vcControllerTest";

    private String authHeader;

    public VirtualCorpusControllerTest () throws KustvaktException {
        authHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(testUser, "pass");
    }

    @Test
    public void testDeleteVC_unauthorized () throws KustvaktException {
    	createDoryVC();
        Response response = target().path(API_VERSION).path("vc").path("~dory")
                .path("dory-vc").request()
                .header(Attributes.AUTHORIZATION, authHeader).delete();
        testResponseUnauthorized(response, testUser);
        deleteVC("dory-vc", "dory", "dory");
    }
    
    private void testDeleteSystemVC (String vcName) throws KustvaktException {
        Response response = deleteVC(vcName, "system", "admin");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testDeleteSystemVC_unauthorized (String vcName,
            String username) throws KustvaktException {
        Response response = deleteVC(vcName, "system", username);
        testResponseUnauthorized(response, "dory");
    }
    
    @Test
    public void testCreatePrivateVC () throws KustvaktException {
        createPrivateVC(testUser, "new_vc");
        
        // list user VC
        JsonNode node = listVC(testUser);
        assertEquals(2, node.size());
        assertEquals(node.get(1).get("name").asText(), "new_vc");
        
        testCreateVC_sameName(testUser, "new_vc", ResourceType.PRIVATE);
        
        // delete new VC
        deleteVC("new_vc", testUser, testUser);
        // list VC
        node = listVC(testUser);
        assertEquals(1, node.size());
    }
    
    @Test
    public void testCreateSystemVC () throws KustvaktException {
        String json = "{\"type\": \"SYSTEM\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"pubDate since 1820\"}";
        String vcName = "new_system_vc";
        Response response = target().path(API_VERSION).path("vc")
                .path("~system").path(vcName).request()
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
        
        testDeleteSystemVC_unauthorized(vcName, "dory");
        testDeleteSystemVC(vcName);
        
        node = listSystemVC("pearl");
        assertEquals(1, node.size());
    }

    @Test
    public void testCreateSystemVC_unauthorized () throws KustvaktException {
        String json = "{\"type\": \"SYSTEM\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + testUser).path("new_vc").request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        testResponseUnauthorized(response, testUser);
    }

    
    private void testCreateVC_sameName (String username, String vcName,
            ResourceType vcType) throws KustvaktException {
        String vcJson = "{\"type\": \"" + vcType + "\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";

        String authHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, "pass");

        Response response = target().path(API_VERSION).path("vc")
                .path("~" + username).path(vcName).request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(vcJson));

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testCreateVC_invalidToken ()
            throws IOException, KustvaktException {
        String json = "{\"type\": \"PRIVATE\","
                + "\"corpusQuery\": \"corpusSigle=GOE\"}";
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("test-invalid-signature.token");
        String authToken;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is))) {
            authToken = reader.readLine();
        }
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + testUser).path("new_vc").request()
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
        assertEquals(node.at("/errors/0/1").asText(),
                "Access token is invalid");
        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateVC_expiredToken ()
            throws IOException, KustvaktException {
        String json = "{\"type\": \"PRIVATE\","
                + "\"corpusQuery\": \"corpusSigle=GOE\"}";

        //String authToken = "fia0123ikBWn931470H8s5gRqx7Moc4p";
        String authToken = createExpiredAccessToken();

        Response response = target().path(API_VERSION).path("vc")
                .path("~marlin").path("new_vc").request()
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
        assertEquals(node.at("/errors/0/1").asText(),
                "Access token is expired");
        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateVC_invalidName () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + testUser).path("new $vc").request()
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
                .path("~" + testUser).path("ne").request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "queryName must contain at least 3 characters");
    }

    @Test
    public void testCreateVC_unauthorized () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\","
                + "\"corpusQuery\": \"creationDate since 1820\"}";
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + testUser).path("new_vc").request()
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "Unauthorized operation for user: guest");
        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateVC_withoutCorpusQuery () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\"" + "}";
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + testUser).path("new_vc").request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(), "corpusQuery is null");
        assertEquals(node.at("/errors/0/2").asText(), "corpusQuery");
    }

    @Test
    public void testCreateVC_withoutEntity () throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + testUser).path("new_vc").request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(""));
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(), "request entity is null");
        assertEquals(node.at("/errors/0/2").asText(), "request entity");
    }

    @Test
    public void testCreateVC_withoutType () throws KustvaktException {
        String json = "{\"corpusQuery\": " + "\"creationDate since 1820\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\"" + "}";
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + testUser).path("new_vc").request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(), "type is null");
        assertEquals(node.at("/errors/0/2").asText(), "type");
    }

    @Test
    public void testCreateVC_withWrongType () throws KustvaktException {
        String json = "{\"type\": \"PRIVAT\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + testUser).path("new_vc").request()
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
    public void testDetermineCorpusAccess () throws KustvaktException {
        String vcName = "vc-all";
        createPrivateVC(testUser, vcName);
        JsonNode node = listVC(testUser);
        assertEquals(2, node.size());
        
        node = node.get(1);
        assertEquals(vcName, node.get("name").asText());
        assertEquals("ALL", node.get("requiredAccess").asText());
        deleteVC(vcName, testUser, testUser);
        

        vcName = "vc-pub-1";
        String vcJson = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"availability=/ACA.*/\"}";

        createVC(authHeader, testUser, vcName, vcJson);
        node = listVC(testUser).get(1);
        assertEquals(vcName, node.get("name").asText());
        assertEquals("PUB", node.get("requiredAccess").asText());
        deleteVC(vcName, testUser, testUser);
        
        vcName = "vc-pub-2";
        vcJson = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"availability=QAO-NC\"}";

        createVC(authHeader, testUser, vcName, vcJson);
        node = listVC(testUser).get(1);
        assertEquals(vcName, node.get("name").asText());
        assertEquals("PUB", node.get("requiredAccess").asText());
        deleteVC(vcName, testUser, testUser);
        
        vcName = "vc-free";
        vcJson = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"availability=/CC.*/\"}";

        createVC(authHeader, testUser, vcName, vcJson);
        node = listVC(testUser).get(1);
        assertEquals(vcName, node.get("name").asText());
        assertEquals("FREE", node.get("requiredAccess").asText());
        deleteVC(vcName, testUser, testUser);
    }
    
    @Test
    public void testMaxNumberOfVC () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";
        for (int i = 1; i < 6; i++) {
            createPrivateVC(testUser, "new_vc_" + i);
        }
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + testUser).path("new_vc_6").request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NOT_ALLOWED, node.at("/errors/0/0").asInt());
        assertEquals(
                "Cannot create virtual corpus. The maximum number of "
                        + "virtual corpus has been reached.",
                node.at("/errors/0/1").asText());
        // list user VC
        node = listVC(testUser);
        // including 1 system-vc
        assertEquals(6, node.size());
        // delete new VC
        for (int i = 1; i < 6; i++) {
            deleteVC("new_vc_" + i, testUser, testUser);
        }
        // list VC
        node = listVC(testUser);
        // system-vc
        assertEquals(1, node.size());
    }

    @Test
    public void testEditVC () throws KustvaktException {
    	createDoryVC();
        // 1st edit
        String json = "{\"description\": \"edited vc\"}";
        editVC("dory", "dory", "dory-vc", json);
        // check VC
        JsonNode node = retrieveVCInfo("dory", "dory", "dory-vc");
        assertEquals(node.at("/description").asText(), "edited vc");
        // 2nd edit
        json = "{\"description\": \"test vc\"}";
        editVC("dory", "dory", "dory-vc", json);
        // check VC
        node = retrieveVCInfo("dory", "dory", "dory-vc");
        assertEquals(node.at("/description").asText(), "test vc");
        deleteVC("dory-vc", "dory", "dory");
    }

    @Test
    public void testEditVCName () throws KustvaktException {
        String json = "{\"name\": \"new-name\"}";
        Response response = target().path(API_VERSION).path("vc").path("~dory")
                .path("dory-vc").request()
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
    public void testEditCorpusQuery ()
            throws ProcessingException, KustvaktException {
    	createDoryVC();
        JsonNode node = testRetrieveKoralQuery("dory", "dory-vc");
        node = node.at("/"+COLLECTION_NODE_NAME);
        assertEquals(node.at("/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/operation").asText(),
                "operation:and");
        assertEquals(2, node.at("/operands").size());
        String json = "{\"corpusQuery\": \"corpusSigle=WPD17\"}";
        editVC("dory", "dory", "dory-vc", json);
        
        node = testRetrieveKoralQuery("dory", "dory-vc");
        node = node.at("/"+COLLECTION_NODE_NAME);
        assertEquals(node.at("/@type").asText(), "koral:doc");
        assertEquals(node.at("/key").asText(), "corpusSigle");
        assertEquals(node.at("/value").asText(), "WPD17");
        
        json = "{\"corpusQuery\": \"corpusSigle=GOE AND creationDate since "
                + "1820\"}";
        editVC("dory", "dory", "dory-vc", json);
        node = testRetrieveKoralQuery("dory", "dory-vc");
        node = node.at("/"+COLLECTION_NODE_NAME);
        assertEquals(node.at("/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/operation").asText(),
                "operation:and");
        assertEquals(node.at("/operands/0/key").asText(),
                "corpusSigle");
        assertEquals(node.at("/operands/0/value").asText(), "GOE");
        assertEquals(node.at("/operands/1/key").asText(),
                "creationDate");
        assertEquals(node.at("/operands/1/value").asText(), "1820");
        
        deleteVC("dory-vc", "dory", "dory");
    }

    private JsonNode testRetrieveKoralQuery (String username, String vcName)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("koralQuery").path("~" + username).path(vcName).request()
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
        Response response = target().path(API_VERSION).path("vc").path("~dory")
                .path("dory-vc").request()
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
}
