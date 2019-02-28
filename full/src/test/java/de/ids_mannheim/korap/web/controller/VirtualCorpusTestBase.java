package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public abstract class VirtualCorpusTestBase extends SpringJerseyTest{
    
    protected JsonNode testSearchVC (String username, String vcCreator, String vcName)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("vc")
                .path(vcCreator).path(vcName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        // System.out.println(entity);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        return JsonUtils.readTree(entity);
    }
    
    protected void testEditVCType (String username, String vcCreator,
            String vcName, VirtualCorpusType type)
            throws KustvaktException {
        String json = "{\"type\": \"" + type + "\"}";

        ClientResponse response = resource().path(API_VERSION).path("vc")
                .path(vcCreator).path(vcName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(ClientResponse.class, json);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = testSearchVC(username, vcCreator, vcName);
        assertEquals(type.displayName(), node.at("/type").asText());
    }
}
