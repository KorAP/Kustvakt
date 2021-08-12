package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.JsonUtils;

public class QueryReferenceControllerTest extends SpringJerseyTest {

    private String testUser = "qRefControllerTest";
    private String adminUser = "admin";

    private void checkAndDeleteQuery (JsonNode node, String qName, String query,
            String username, ResourceType resourceType, CorpusAccess access)
            throws KustvaktException {
        assertEquals(qName, node.at("/name").asText());
        assertEquals(resourceType.displayName(), node.at("/type").asText());
        assertEquals(username, node.at("/createdBy").asText());
        assertEquals(query, node.at("/query").asText());
        assertEquals("poliqarp", node.at("/queryLanguage").asText());
        assertEquals(access.name(), node.at("/requiredAccess").asText());

        testDeleteQueryByName(qName, username);
    }
    
    @Test
    public void testCreatePrivateQuery () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\"" 
                + ",\"queryType\": \"QUERY\""
                + ",\"queryLanguage\": \"poliqarp\"" 
                + ",\"query\": \"der\"}";

        String qName = "new_query";
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~" + testUser).path(qName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(ClientResponse.class, json);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = testRetrieveQueryByName(testUser, testUser, qName);
        checkAndDeleteQuery(node, qName, "der", testUser, ResourceType.PRIVATE,
                CorpusAccess.PUB);
    }

    @Test
    public void testCreateUserQueryByAdmin () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"QUERY\""
                + ",\"queryLanguage\": \"poliqarp\"" 
                + ",\"query\": \"Sommer\"}";

        String qName = "marlin-query";
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~marlin").path(qName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).put(ClientResponse.class);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        testDeleteQueryByName(qName, "admin");
    }
    
    @Test
    public void testCreateSystemQuery () throws KustvaktException {
        String json = "{\"type\": \"SYSTEM\""
                + ",\"queryType\": \"QUERY\""
                + ",\"queryLanguage\": \"poliqarp\"" 
                + ",\"query\": \"Sommer\"}";

        String qName = "system-query";
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~system").path(qName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).put(ClientResponse.class);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        testDeleteQueryByName(qName, "admin");
    }
    
    @Test
    public void testCreateSystemQueryUnauthorized () throws KustvaktException {
        String json = "{\"type\": \"SYSTEM\""
                + ",\"queryType\": \"QUERY\""
                + ",\"queryLanguage\": \"poliqarp\"" 
                + ",\"query\": \"Sommer\"}";

        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~"+testUser).path("system-query")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).put(ClientResponse.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: " + testUser,
                node.at("/errors/0/1").asText());
    }
    
    @Test
    public void testCreateQueryMissingQueryType () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\"" 
                + ",\"queryLanguage\": \"poliqarp\""
                + ",\"query\": \"Sohn\"}";

        String qName = "new_query";
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~" + testUser).path(qName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(ClientResponse.class, json);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = testRetrieveQueryByName(testUser, testUser, qName);
        checkAndDeleteQuery(node, qName, "Sohn", testUser, ResourceType.PRIVATE,
                CorpusAccess.PUB);
    }
    
    @Test
    public void testCreateQueryMissingQueryLanguage () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\"" 
                + ",\"queryType\": \"QUERY\""
                + ",\"query\": \"Sohn\"}";

        String qName = "new_query";
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~" + testUser).path(qName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(ClientResponse.class, json);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
        assertEquals("queryLanguage is null", node.at("/errors/0/1").asText());
        assertEquals("queryLanguage", node.at("/errors/0/2").asText());
    }
    
    @Test
    public void testCreateQueryMissingQuery () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\"" 
                + ",\"queryType\": \"QUERY\""
                + ",\"queryLanguage\": \"poliqarp\"}";

        String qName = "new_query";
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~" + testUser).path(qName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(ClientResponse.class, json);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
        assertEquals("query is null", node.at("/errors/0/1").asText());
        assertEquals("query", node.at("/errors/0/2").asText());
    }
    
    @Test
    public void testCreateQueryMissingResourceType () throws KustvaktException {
        String json = "{\"query\": \"Wind\""
                + ",\"queryLanguage\": \"poliqarp\"}";

        String qName = "new_query";
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~" + testUser).path(qName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(ClientResponse.class, json);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
        assertEquals("type is null", node.at("/errors/0/1").asText());
        assertEquals("type", node.at("/errors/0/2").asText());
    }
    
    @Test
    public void testDeleteQueryUnauthorized () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~dory").path("dory-q")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .delete(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: " + testUser,
                node.at("/errors/0/1").asText());
    }
    
    @Test
    public void testDeleteNonExistingQuery () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~dory").path("non-existing-query")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Query dory/non-existing-query is not found.",
                node.at("/errors/0/1").asText());
        assertEquals("dory/non-existing-query",
                node.at("/errors/0/2").asText());
    }

    @Test
    public void testAvailableQueryForDory () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode node = testListAvailableQuery("dory");
        assertEquals(2, node.size());
    }

    @Test
    public void testListAvailableQueryForPearl ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        
        JsonNode node = testListAvailableQuery("pearl");
        
        assertEquals(1, node.size());
        assertEquals("system-q", node.at("/0/name").asText());
        assertEquals(ResourceType.SYSTEM.displayName(),
                node.at("/0/type").asText());
        assertEquals("\"system\" query", node.at("/0/description").asText());
        assertEquals("koral:token", node.at("/0/koralQuery/@type").asText());

    }

    private JsonNode testListAvailableQuery (String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("query")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }
    
    private JsonNode testRetrieveQueryByName (String username, String qCreator,
            String qName) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~" + qCreator).path(qName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        // System.out.println(entity);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        return JsonUtils.readTree(entity);
    }

    private void testDeleteQueryByName (String qName, String username)
            throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~" + username).path(qName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

}
