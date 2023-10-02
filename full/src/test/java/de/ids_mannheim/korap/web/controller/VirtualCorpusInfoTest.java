package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VirtualCorpusInfoTest extends VirtualCorpusTestBase {

    private String admin = "admin";

    private String testUser = "VirtualCorpusInfoTest";

    @Test
    public void testRetrieveSystemVC() throws ProcessingException, KustvaktException {
        JsonNode node = retrieveVCInfo(testUser, "system", "system-vc");
        assertEquals(node.at("/name").asText(), "system-vc");
        assertEquals(ResourceType.SYSTEM.displayName(), node.at("/type").asText());
        // assertEquals("koral:doc", node.at("/koralQuery/collection/@type").asText());
        assertTrue(node.at("/query").isMissingNode());
        assertTrue(node.at("/queryLanguage").isMissingNode());
    }

    @Test
    public void testRetrieveSystemVCGuest() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~system").path("system-vc").request().get();
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(node.at("/name").asText(), "system-vc");
        assertEquals(ResourceType.SYSTEM.displayName(), node.at("/type").asText());
    }

    @Test
    public void testRetrieveOwnerPrivateVC() throws ProcessingException, KustvaktException {
        JsonNode node = retrieveVCInfo("dory", "dory", "dory-vc");
        assertEquals(node.at("/name").asText(), "dory-vc");
        assertEquals(ResourceType.PRIVATE.displayName(), node.at("/type").asText());
    }

    @Test
    public void testRetrievePrivateVCUnauthorized() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory").path("dory-vc").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(testUser, "pass")).get();
        testResponseUnauthorized(response, testUser);
    }

    @Test
    public void testRetrieveProjectVC() throws ProcessingException, KustvaktException {
        JsonNode node = retrieveVCInfo("nemo", "dory", "group-vc");
        assertEquals(node.at("/name").asText(), "group-vc");
        assertEquals(ResourceType.PROJECT.displayName(), node.at("/type").asText());
    }

    @Test
    public void testRetrieveProjectVCUnauthorized() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory").path("group-vc").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(testUser, "pass")).get();
        testResponseUnauthorized(response, testUser);
    }

    @Test
    public void testRetrieveProjectVCbyNonActiveMember() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory").path("group-vc").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("marlin", "pass")).get();
        testResponseUnauthorized(response, "marlin");
    }

    @Test
    public void testRetrievePublishedVC() throws ProcessingException, KustvaktException {
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
    public void testAdminRetrievePrivateVC() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory").path("dory-vc").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.at("/id").asInt());
        assertEquals(node.at("/name").asText(), "dory-vc");
    }

    @Test
    public void testAdminRetrieveProjectVC() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory").path("group-vc").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(admin, "pass")).get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/name").asText(), "group-vc");
        assertEquals(ResourceType.PROJECT.displayName(), node.at("/type").asText());
    }
}
