package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.client.Entity;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public abstract class VirtualCorpusTestBase extends SpringJerseyTest{
    
    protected JsonNode testSearchVC (String username, String vcCreator, String vcName)
            throws ProcessingException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("vc")
                .path("~"+vcCreator).path(vcName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();
        String entity = response.readEntity(String.class);
        // System.out.println(entity);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        return JsonUtils.readTree(entity);
    }
    
    protected void testEditVCType (String username, String vcCreator,
            String vcName, ResourceType type)
            throws KustvaktException {
        String json = "{\"type\": \"" + type + "\"}";

        Response response = target().path(API_VERSION).path("vc")
                .path("~"+vcCreator).path(vcName)
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(Entity.json(json));

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        JsonNode node = testSearchVC(username, vcCreator, vcName);
        assertEquals(type.displayName(), node.at("/type").asText());
    }
}
