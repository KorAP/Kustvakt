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
    public void testCreatePrivateVC () throws KustvaktException {
        createPrivateVC(testUser, "new_vc");
        
        // list user VC
        JsonNode node = listVC(testUser);
        assertEquals(2, node.size());
        assertEquals(node.get(1).get("name").asText(), "new_vc");
        // delete new VC
        deleteVC("new_vc", testUser, testUser);
        // list VC
        node = listVC(testUser);
        assertEquals(1, node.size());
    }

    @Test
    public void testCreateVCWithInvalidToken ()
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
    public void testCreateVCWithExpiredToken ()
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
        deleteVC(vcName, "system", "admin");
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
    public void testDeleteVC_unauthorized () throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory")
                .path("dory-vc").request()
                .header(Attributes.AUTHORIZATION, authHeader).delete();
        testResponseUnauthorized(response, testUser);
    }

    @Test
    public void testEditVC () throws KustvaktException {
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
        JsonNode node = testRetrieveKoralQuery("dory", "dory-vc");
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operation").asText(),
                "operation:and");
        assertEquals(2, node.at("/collection/operands").size());
        String json = "{\"corpusQuery\": \"corpusSigle=WPD17\"}";
        editVC("dory", "dory", "dory-vc", json);
        node = testRetrieveKoralQuery("dory", "dory-vc");
        assertEquals(node.at("/collection/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/key").asText(), "corpusSigle");
        assertEquals(node.at("/collection/value").asText(), "WPD17");
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
