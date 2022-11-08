package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.client.Entity;

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
    private String system = "system";

    private void testRetrieveQueryByName (String qName, String query,
            String queryCreator, String username, ResourceType resourceType,
            CorpusAccess access) throws KustvaktException {
        
        Response response = target().path(API_VERSION).path("query")
                .path("~" + queryCreator).path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals(qName, node.at("/name").asText());
        assertEquals(resourceType.displayName(), node.at("/type").asText());
        assertEquals(queryCreator, node.at("/createdBy").asText());
        assertEquals(query, node.at("/query").asText());
        assertEquals("poliqarp", node.at("/queryLanguage").asText());
        assertEquals(access.name(), node.at("/requiredAccess").asText());
    }
    
    private void testUpdateQuery (String qName, String qCreator,
            String username, ResourceType type)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        String json = "{\"query\": \"Sonne\""
                + ",\"queryLanguage\": \"poliqarp\"}";
        
        Response response = target().path(API_VERSION).path("query")
                .path("~"+qCreator).path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));
        
        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        
        testRetrieveQueryByName(qName, "Sonne", qCreator, username, type, CorpusAccess.PUB);
    }
    
    @Test
    public void testCreatePrivateQuery () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\"" 
                + ",\"queryType\": \"QUERY\""
                + ",\"queryLanguage\": \"poliqarp\"" 
                + ",\"query\": \"der\"}";

        String qName = "new_query";
        Response response = target().path(API_VERSION).path("query")
                .path("~" + testUser).path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        testRetrieveQueryByName(qName, "der", testUser, testUser, ResourceType.PRIVATE,
                CorpusAccess.PUB);
        
        testUpdateQuery(qName, testUser, testUser,ResourceType.PRIVATE);
        testDeleteQueryByName(qName, testUser, testUser);
    }

    @Test
    public void testCreatePublishQuery () throws KustvaktException {
        String json = "{\"type\": \"PUBLISHED\"" 
                + ",\"queryType\": \"QUERY\""
                + ",\"queryLanguage\": \"poliqarp\"" 
                + ",\"query\": \"Regen\"}";

        String qName = "publish_query";
        Response response = target().path(API_VERSION).path("query")
                .path("~" + testUser).path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        testRetrieveQueryByName(qName, "Regen", testUser, testUser, ResourceType.PUBLISHED,
                CorpusAccess.PUB);
        testDeleteQueryByName(qName, testUser, testUser);
        // check if hidden group has been created
        
    }
    
    @Test
    public void testCreateUserQueryByAdmin () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"QUERY\""
                + ",\"queryLanguage\": \"poliqarp\"" 
                + ",\"query\": \"Sommer\"}";

        String qName = "marlin-query";
        Response response = target().path(API_VERSION).path("query")
                .path("~marlin").path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        
        testRetrieveQueryByName(qName, "Sommer", "marlin", adminUser, ResourceType.PRIVATE, CorpusAccess.PUB);
        
        testUpdateQuery(qName, "marlin", adminUser, ResourceType.PRIVATE);
        testDeleteQueryByName(qName, "marlin", adminUser);
    }
    
    @Test
    public void testCreateSystemQuery () throws KustvaktException {
        String json = "{\"type\": \"SYSTEM\""
                + ",\"queryType\": \"QUERY\""
                + ",\"queryLanguage\": \"poliqarp\"" 
                + ",\"query\": \"Sommer\"}";

        String qName = "system-query";
        Response response = target().path(API_VERSION).path("query")
                .path("~system").path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(adminUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        
        testRetrieveQueryByName(qName, "Sommer", system, adminUser, ResourceType.SYSTEM, CorpusAccess.PUB);
        testUpdateQuery(qName, system, adminUser, ResourceType.SYSTEM);
        testDeleteSystemQueryUnauthorized(qName);
        testDeleteQueryByName(qName, system, adminUser);
    }
    
    @Test
    public void testCreateSystemQueryUnauthorized () throws KustvaktException {
        String json = "{\"type\": \"SYSTEM\""
                + ",\"queryType\": \"QUERY\""
                + ",\"queryLanguage\": \"poliqarp\"" 
                + ",\"query\": \"Sommer\"}";

        Response response = target().path(API_VERSION).path("query")
                .path("~"+testUser).path("system-query")
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
    }
    
    @Test
    public void testCreateQueryMissingQueryType () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\"" 
                + ",\"queryLanguage\": \"poliqarp\""
                + ",\"query\": \"Sohn\"}";

        String qName = "new_query";
        Response response = target().path(API_VERSION).path("query")
                .path("~" + testUser).path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        testRetrieveQueryByName(qName, "Sohn", testUser, testUser, ResourceType.PRIVATE,
                CorpusAccess.PUB);
        testDeleteQueryByName(qName, testUser, testUser);
    }
    
    @Test
    public void testCreateQueryMissingQueryLanguage () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\"" 
                + ",\"queryType\": \"QUERY\""
                + ",\"query\": \"Sohn\"}";

        String qName = "new_query";
        Response response = target().path(API_VERSION).path("query")
                .path("~" + testUser).path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
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
        Response response = target().path(API_VERSION).path("query")
                .path("~" + testUser).path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
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
        Response response = target().path(API_VERSION).path("query")
                .path("~" + testUser).path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
        assertEquals("type is null", node.at("/errors/0/1").asText());
        assertEquals("type", node.at("/errors/0/2").asText());
    }
    
    private void testDeleteQueryByName (String qName, String qCreator, String username)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("query")
                .path("~" + qCreator).path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testDeleteQueryUnauthorized () throws KustvaktException {
        Response response = target().path(API_VERSION).path("query")
                .path("~dory").path("dory-q")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .delete();

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: " + testUser,
                node.at("/errors/0/1").asText());
    }
    
    private void testDeleteSystemQueryUnauthorized (String qName) throws KustvaktException {
        Response response = target().path(API_VERSION).path("query")
                .path("~system").path(qName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .delete();

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: " + testUser,
                node.at("/errors/0/1").asText());
    }
    
    @Test
    public void testDeleteNonExistingQuery () throws KustvaktException {
        Response response = target().path(API_VERSION).path("query")
                .path("~dory").path("non-existing-query")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .delete();

        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Query dory/non-existing-query is not found.",
                node.at("/errors/0/1").asText());
        assertEquals("dory/non-existing-query",
                node.at("/errors/0/2").asText());
    }

    @Test
    public void testListAvailableQueryForDory () throws UniformInterfaceException,
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
        assertEquals("[]", node.at("/0/query").asText());
        assertEquals(CorpusAccess.FREE.name(), node.at("/0/requiredAccess").asText());
//        assertEquals("koral:token", node.at("/0/koralQuery/@type").asText());
    }

    private JsonNode testListAvailableQuery (String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        Response response = target().path(API_VERSION).path("query")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }
    
}
