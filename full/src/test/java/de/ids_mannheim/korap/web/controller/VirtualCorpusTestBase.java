package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public abstract class VirtualCorpusTestBase extends OAuth2TestBase {

    protected JsonNode retrieveVCInfo (String username, String vcCreator,
            String vcName) throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + vcCreator).path(vcName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        return JsonUtils.readTree(entity);
    }

    protected void createVC (String authHeader,String username, String vcName, 
            String vcJson) throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + username).path(vcName).request()
                .header(Attributes.AUTHORIZATION, authHeader)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(vcJson));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    protected void editVCType (String username, String vcCreator, String vcName,
            ResourceType type) throws KustvaktException {
        String json = "{\"type\": \"" + type + "\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~" + vcCreator).path(vcName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        JsonNode node = retrieveVCInfo(username, vcCreator, vcName);
        assertEquals(type.displayName(), node.at("/type").asText());
    }

    protected JsonNode listVC (String username)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        return JsonUtils.readTree(entity);
    }
    
    protected JsonNode listVCWithAuthHeader (String authHeader)
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").request()
                .header(Attributes.AUTHORIZATION, authHeader).get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    protected Response testShareVCByCreator (String vcCreator, String vcName,
            String groupName) throws ProcessingException, KustvaktException {

        return target().path(API_VERSION).path("vc").path("~" + vcCreator)
                .path(vcName).path("share").path("@" + groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(vcCreator, "pass"))
                .post(Entity.form(new Form()));
    }

    protected JsonNode listAccessByGroup (String username, String groupName)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("access")
                .queryParam("groupName", groupName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    protected void deleteVC (String vcName, String vcCreator, String username)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~" + vcCreator).path(vcName).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
}
