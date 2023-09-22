package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Virtual Corpus Info Test")
class VirtualCorpusInfoTest extends VirtualCorpusTestBase {

    private String admin = "admin";

    private String testUser = "VirtualCorpusInfoTest";

    @Test
    @DisplayName("Test Retrieve System VC")
    void testRetrieveSystemVC() throws ProcessingException, KustvaktException {
        JsonNode node = retrieveVCInfo(testUser, "system", "system-vc");
        assertEquals(node.at("/name").asText(), "system-vc");
        assertEquals(ResourceType.SYSTEM.displayName(), node.at("/type").asText());
        // assertEquals("koral:doc", node.at("/koralQuery/collection/@type").asText());
        assertTrue(node.at("/query").isMissingNode());
        assertTrue(node.at("/queryLanguage").isMissingNode());
    }

    @Test
    @DisplayName("Test Retrieve System VC Guest")
    void testRetrieveSystemVCGuest() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~system").path("system-vc").request().get();
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(node.at("/name").asText(), "system-vc");
        assertEquals(ResourceType.SYSTEM.displayName(), node.at("/type").asText());
    }

    @Test
    @DisplayName("Test Retrieve Owner Private VC")
    void testRetrieveOwnerPrivateVC() throws ProcessingException, KustvaktException {
        JsonNode node = retrieveVCInfo("dory", "dory", "dory-vc");
        assertEquals(node.at("/name").asText(), "dory-vc");
        assertEquals(ResourceType.PRIVATE.displayName(), node.at("/type").asText());
    }

    @Test
    @DisplayName("Test Retrieve Private VC Unauthorized")
    void testRetrievePrivateVCUnauthorized() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory").path("dory-vc").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(testUser, "pass")).get();
        testResponseUnauthorized(response, testUser);
    }

    @Test
    @DisplayName("Test Retrieve Project VC")
    void testRetrieveProjectVC() throws ProcessingException, KustvaktException {
        JsonNode node = retrieveVCInfo("nemo", "dory", "group-vc");
        assertEquals(node.at("/name").asText(), "group-vc");
        assertEquals(ResourceType.PROJECT.displayName(), node.at("/type").asText());
    }

    @Test
    @DisplayName("Test Retrieve Project VC Unauthorized")
    void testRetrieveProjectVCUnauthorized() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory").path("group-vc").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(testUser, "pass")).get();
        testResponseUnauthorized(response, testUser);
    }

    @Test
    @DisplayName("Test Retrieve Project V Cby Non Active Member")
    void testRetrieveProjectVCbyNonActiveMember() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory").path("group-vc").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("marlin", "pass")).get();
        testResponseUnauthorized(response, "marlin");
    }

    @Test
    @DisplayName("Test Retrieve Published VC")
    void testRetrievePublishedVC() throws ProcessingException, KustvaktException {
        JsonNode node = retrieveVCInfo("gill", "marlin", "published-vc");
        assertEquals(node.at("/name").asText(), "published-vc");
        assertEquals(ResourceType.PUBLISHED.displayName(), node.at("/type").asText());
        Form f = new Form();
        f.param("status", "HIDDEN");
        // check gill in the hidden group of the vc
        Response response = target().path(API_VERSION).path("admin").path("group").path("list").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("admin", "pass")).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED).post(Entity.form(f));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(3, node.at("/0/id").asInt());
        String members = node.at("/0/members").toString();
        assertTrue(members.contains("\"userId\":\"gill\""));
    }

    @Test
    @DisplayName("Test Admin Retrieve Private VC")
    void testAdminRetrievePrivateVC() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory").path("dory-vc").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.at("/id").asInt());
        assertEquals(node.at("/name").asText(), "dory-vc");
    }

    @Test
    @DisplayName("Test Admin Retrieve Project VC")
    void testAdminRetrieveProjectVC() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory").path("group-vc").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/name").asText(), "group-vc");
        assertEquals(ResourceType.PROJECT.displayName(), node.at("/type").asText());
    }
}
