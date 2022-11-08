package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.client.Entity;
import org.glassfish.jersey.server.ContainerRequest;

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

    private void checkWWWAuthenticateHeader (Response response) {
        Set<Entry<String, List<Object>>> headers =
                response.getHeaders().entrySet();

        for (Entry<String, List<Object>> header : headers) {
            if (header.getKey().equals(ContainerRequest.WWW_AUTHENTICATE)) {
                assertEquals("Api realm=\"Kustvakt\"",
                        header.getValue().get(0));
                assertEquals("Bearer realm=\"Kustvakt\"",
                        header.getValue().get(1));
                assertEquals("Basic realm=\"Kustvakt\"",
                        header.getValue().get(2));
            }
        }
    }

    private JsonNode testListVC (String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        return JsonUtils.readTree(entity);
    }

    private JsonNode testListOwnerVC (String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + username)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    private void testDeleteVC (String vcName, String vcCreator, String username)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + vcCreator).path(vcName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private JsonNode testlistAccessByGroup (String username, String groupName)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("access").queryParam("groupName", groupName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    @Test
    public void testRetrieveSystemVCInfo () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        JsonNode node = testSearchVC(testUser, "system", "system-vc");
        assertEquals("system-vc", node.at("/name").asText());
        assertEquals(ResourceType.SYSTEM.displayName(),
                node.at("/type").asText());
//        assertEquals("koral:doc", node.at("/koralQuery/collection/@type").asText());
        assertTrue(node.at("/query").isMissingNode());
        assertTrue(node.at("/queryLanguage").isMissingNode());
    }

    @Test
    public void testRetrieveSystemVCGuest () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        Response response = target().path(API_VERSION).path("vc")
                .path("~system").path("system-vc")
                .request()
                .get();
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals("system-vc", node.at("/name").asText());
        assertEquals(ResourceType.SYSTEM.displayName(),
                node.at("/type").asText());
//        assertEquals(11, node.at("/numberOfDoc").asInt());
//        assertEquals(772, node.at("/numberOfParagraphs").asInt());
//        assertEquals(25074, node.at("/numberOfSentences").asInt());
//        assertEquals(665842, node.at("/numberOfTokens").asInt());
    }

    @Test
    public void testRetrieveOwnerPrivateVCInfo ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        JsonNode node = testSearchVC("dory", "dory", "dory-vc");
        assertEquals("dory-vc", node.at("/name").asText());
        assertEquals(ResourceType.PRIVATE.displayName(),
                node.at("/type").asText());
    }

    @Test
    public void testRetrievePrivateVCInfoUnauthorized ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path("dory-vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();
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
    public void testRetrieveProjectVCInfo () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        JsonNode node = testSearchVC("nemo", "dory", "group-vc");
        assertEquals("group-vc", node.at("/name").asText());
        assertEquals(ResourceType.PROJECT.displayName(),
                node.at("/type").asText());
    }

    @Test
    public void testRetrieveProjectVCInfoByNonActiveMember ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path("group-vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("marlin", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: marlin",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testRetrievePublishedVCInfo () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        JsonNode node = testSearchVC("gill", "marlin", "published-vc");
        assertEquals("published-vc", node.at("/name").asText());
        assertEquals(ResourceType.PUBLISHED.displayName(),
                node.at("/type").asText());

        // check gill in the hidden group of the vc
        Response response = target().path(API_VERSION).path("group")
                .path("list").path("system-admin")
                .queryParam("status", "HIDDEN")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(3, node.at("/0/id").asInt());
        String members = node.at("/0/members").toString();
        assertTrue(members.contains("\"userId\":\"gill\""));
    }

    @Test
    public void testListAvailableVCNemo () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode node = testListVC("nemo");
        assertEquals(3, node.size());

    }

    @Test
    public void testListAvailableVCPearl () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode node = testListVC("pearl");
        assertEquals(2, node.size());

    }

    @Test
    public void testListAvailableVCDory () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode node = testListVC("dory");
        assertEquals(4, node.size());
    }

    @Test
    public void testListAvailableVCByOtherUser ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~dory")
                .request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("pearl", "pass"))
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: pearl",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testListAvailableVCByGuest () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .request()
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: guest",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }
    
    private void testListSystemVC () throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~system")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("pearl", "pass"))
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
        assertEquals(ResourceType.SYSTEM.displayName(),
                node.at("/0/type").asText());
        assertEquals(ResourceType.SYSTEM.displayName(),
                node.at("/1/type").asText());
    }

    @Test
    public void testCreatePrivateVC () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        // list user VC
        JsonNode node = testListVC(testUser);
        assertEquals(2, node.size());
        assertEquals("new_vc", node.get(1).get("name").asText());

        // delete new VC
        testDeleteVC("new_vc", testUser, testUser);

        // list VC
        node = testListVC(testUser);
        assertEquals(1, node.size());
    }

    @Test
    public void testCreatePublishedVC () throws KustvaktException {
        String json = "{\"type\": \"PUBLISHED\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";

        String vcName = "new-published-vc";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path(vcName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        // test list owner vc
        JsonNode node = testListOwnerVC(testUser);
        assertEquals(1, node.size());
        assertEquals(vcName, node.get(0).get("name").asText());

        // EM: check hidden access
        node = testlistAccessByGroup("admin", "");
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
        testDeleteVC(vcName, testUser, testUser);

        // EM: check if the hidden groups are deleted as well
        node = testCheckHiddenGroup(groupName);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Group " + groupName + " is not found",
                node.at("/errors/0/1").asText());
    }

    private JsonNode testCheckHiddenGroup (String groupName)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("group")
                .path("@"+groupName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

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
                        AuthenticationScheme.API.displayName() + " "
                                + authToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        String entity = response.readEntity(String.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Json Web Signature (JWS) object verification failed.",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateVCWithExpiredToken ()
            throws IOException, KustvaktException {
        String json = "{\"type\": \"PRIVATE\","
                + "\"corpusQuery\": \"corpusSigle=GOE\"}";

        String authToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0VXNlci"
                + "IsImlzcyI6Imh0dHBzOlwvXC9rb3JhcC5pZHMtbWFubmhlaW0uZG"
                + "UiLCJleHAiOjE1MzA2MTgyOTR9.JUMvTQZ4tvdRXFBpQKzoNxrq7"
                + "CuYAfytr_LWqY8woJs";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION,
                        AuthenticationScheme.API.displayName() + " "
                                + authToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        String entity = response.readEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.EXPIRED, node.at("/errors/0/0").asInt());
        assertEquals("Authentication token is expired",
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
        
        testListSystemVC();
        testDeleteVC(vcName, "system","admin");
    }        
    
    @Test
    public void testCreateSystemVCUnauthorized () throws KustvaktException {
        String json = "{\"type\": \"SYSTEM\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: " + testUser,
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateVCInvalidName () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new $vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
    }

    
    @Test
    public void testCreateVCNameTooShort () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("ne")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
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
    public void testCreateVCUnauthorized () throws KustvaktException {
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
    public void testCreateVCWithoutcorpusQuery () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + "}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
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
    public void testCreateVCWithoutEntity () throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
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
    public void testCreateVCWithoutType () throws KustvaktException {
        String json = "{\"corpusQuery\": " + "\"creationDate since 1820\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + "}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
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
    public void testCreateVCWithWrongType () throws KustvaktException {
        String json = "{\"type\": \"PRIVAT\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+testUser).path("new_vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
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
    public void testDeleteVCUnauthorized () throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path("dory-vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .delete();

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
    public void testEditVC () throws KustvaktException {

        // 1st edit
        String json = "{\"description\": \"edited vc\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path("dory-vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // check VC
        JsonNode node = testListVC("dory");
        assertEquals("edited vc", node.get(0).get("description").asText());

        // 2nd edit
        json = "{\"description\": \"test vc\"}";

        response = target().path(API_VERSION).path("vc").path("~dory")
                .path("dory-vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // check VC
        node = testListVC("dory");
        assertEquals("test vc", node.get(0).get("description").asText());
    }

    @Test
    public void testEditCorpusQuery () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode node = testRetrieveKoralQuery("dory", "dory-vc");
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and", node.at("/collection/operation").asText());
        assertEquals(2, node.at("/collection/operands").size());
        
        String json = "{\"corpusQuery\": \"corpusSigle=WPD17\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path("dory-vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        node = testRetrieveKoralQuery("dory", "dory-vc");
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("corpusSigle", node.at("/collection/key").asText());
        assertEquals("WPD17", node.at("/collection/value").asText());
    }
    
    private JsonNode testRetrieveKoralQuery (String username, String vcName)
            throws UniformInterfaceException, ClientHandlerException,
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
    public void testEditVCNotOwner () throws KustvaktException {
        String json = "{\"description\": \"edited vc\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path("dory-vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
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
        JsonNode node = testSearchVC("dory", "dory", vcName);
        assertEquals(ResourceType.PROJECT.displayName(),
                node.get("type").asText());

        // edit vc
        String json = "{\"type\": \"PUBLISHED\"}";
        Response response = target().path(API_VERSION).path("vc")
                .path("~dory").path(vcName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // check VC
        node = testListOwnerVC("dory");
        JsonNode n = node.get(1);
        assertEquals(ResourceType.PUBLISHED.displayName(),
                n.get("type").asText());

        // check hidden VC access
        node = testlistAccessByGroup("admin", "");
        assertEquals(4, node.size());
        node = node.get(node.size() - 1);
        assertEquals(vcName, node.at("/queryName").asText());
        assertEquals("system", node.at("/createdBy").asText());
        assertTrue(node.at("/userGroupName").asText().startsWith("auto"));

        // edit 2nd
        json = "{\"type\": \"PROJECT\"}";

        response = target().path(API_VERSION).path("vc").path("~dory")
                .path("group-vc")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        node = testListOwnerVC("dory");
        assertEquals(ResourceType.PROJECT.displayName(),
                node.get(1).get("type").asText());

        // check VC access
        node = testlistAccessByGroup("admin", "");
        assertEquals(3, node.size());
    }

    @Test
    public void testlistAccessByNonVCAAdmin () throws KustvaktException {
        JsonNode node = testlistAccessByGroup("nemo", "dory-group");
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: nemo",
                node.at("/errors/0/1").asText());
    }

    // @Test
    // public void testlistAccessMissingId () throws KustvaktException
    // {
    // Response response =
    // target().path(API_VERSION).path("vc")
    // .path("access")
    // .request().header(Attributes.AUTHORIZATION,
    // HttpAuthorizationHandler
    // .createBasicAuthorizationHeaderValue(
    // testUser, "pass"))
    // .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
    // .get();
    // String entity = response.readEntity(String.class);
    // JsonNode node = JsonUtils.readTree(entity);
    // assertEquals(Status.BAD_REQUEST.getStatusCode(),
    // response.getStatus());
    // assertEquals(StatusCodes.MISSING_PARAMETER,
    // node.at("/errors/0/0").asInt());
    // assertEquals("vcId", node.at("/errors/0/1").asText());
    // }

    @Test
    public void testlistAccessByGroup () throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("access").queryParam("groupName", "dory-group")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.at("/0/accessId").asInt());
        assertEquals(2, node.at("/0/queryId").asInt());
        assertEquals("group-vc", node.at("/0/queryName").asText());
        assertEquals(2, node.at("/0/userGroupId").asInt());

        assertEquals("dory-group", node.at("/0/userGroupName").asText());
    }

    @Test
    public void testCreateDeleteAccess () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        String vcName = "marlin-vc";
        String groupName = "marlin-group";

        // check the vc type
        JsonNode node = testSearchVC("marlin", "marlin", vcName);
        assertEquals(vcName, node.at("/name").asText());
        assertEquals("private", node.at("/type").asText());

        Response response =
                testShareVCByCreator("marlin", vcName, groupName);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check the vc type
        node = testSearchVC("marlin", "marlin", vcName);
        assertEquals("project", node.at("/type").asText());

        // list vc access by marlin
        node = testlistAccessByGroup("marlin", groupName);
        assertEquals(2, node.size());
        node = node.get(1);
        assertEquals(5, node.at("/queryId").asInt());
        assertEquals(vcName, node.at("/queryName").asText());
        assertEquals(1, node.at("/userGroupId").asInt());
        assertEquals(groupName, node.at("/userGroupName").asText());

        String accessId = node.at("/accessId").asText();

        testShareVCNonUniqueAccess("marlin", vcName, groupName);
        testDeleteAccessUnauthorized(accessId);

        // delete access by vc-admin
        // dory is a vc-admin in marlin group
        response = testDeleteAccess("dory", accessId);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // list vc access by dory
        node = testlistAccessByGroup("dory", groupName);
        assertEquals(1, node.size());

        testEditVCType("marlin", "marlin", vcName, ResourceType.PRIVATE);
    }

    private Response testShareVCByCreator (String vcCreator,
            String vcName, String groupName) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        return target().path(API_VERSION).path("vc").path("~"+vcCreator)
                .path(vcName).path("share").path("@"+groupName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(vcCreator, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .post(Entity.form(new Form()));
    }

    private void testShareVCNonUniqueAccess (String vcCreator, String vcName,
            String groupName) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        Response response =
                testShareVCByCreator(vcCreator, vcName, groupName);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        assertEquals(StatusCodes.DB_INSERT_FAILED,
                node.at("/errors/0/0").asInt());

        // EM: message differs depending on the database used
        // for testing. The message below is from sqlite.
        // assertTrue(node.at("/errors/0/1").asText()
        // .startsWith("[SQLITE_CONSTRAINT_UNIQUE]"));
    }

    @Test
    public void testShareUnknownVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        Response response = testShareVCByCreator("marlin",
                "non-existing-vc", "marlin group");
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testShareUnknownGroup () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        Response response = testShareVCByCreator("marlin", "marlin-vc",
                "non-existing-group");
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testShareVCByVCAAdmin () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        // dory is VCA in marlin group
        Response response = target().path(API_VERSION).path("vc")
                .path("~marlin").path("marlin-vc").path("share")
                .path("@marlin group")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .post(Entity.form(new Form()));

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: dory",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testShareVCByNonVCAAdmin () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        // nemo is not VCA in marlin group
        Response response = target().path(API_VERSION).path("vc")
                .path("~nemo").path("nemo-vc").path("share").path("@marlin-group")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("nemo", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .post(Entity.form(new Form()));

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: nemo",
                node.at("/errors/0/1").asText());
    }

    private Response testDeleteAccess (String username, String accessId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("access").path(accessId)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete();

        return response;
    }

    private void testDeleteAccessUnauthorized (String accessId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("access").path(accessId)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete();

        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: " + testUser,
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testDeleteNonExistingAccess () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        Response response = testDeleteAccess("dory", "100");
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
    }
}
