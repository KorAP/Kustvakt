package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.cache.VirtualCorpusCache;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.NamedVCLoader;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VirtualCorpusFieldTest extends VirtualCorpusTestBase {

    @Autowired
    private NamedVCLoader vcLoader;

    private JsonNode testRetrieveField (String username, String vcName,
            String field) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("vc")
                .path("field").path("~" + username).path(vcName)
                .queryParam("fieldName", field)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    private void testRetrieveProhibitedField (String username, String vcName,
            String field) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("vc")
                .path("field").path("~" + username).path(vcName)
                .queryParam("fieldName", field)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .get(ClientResponse.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NOT_ALLOWED, node.at("/errors/0/0").asInt());
    }

    @Test
    public void testRetrieveFieldsNamedVC1 ()
            throws IOException, QueryException, KustvaktException {

        vcLoader.loadVCToCache("named-vc1", "/vc/named-vc1.jsonld");

        JsonNode n = testRetrieveField("system", "named-vc1", "textSigle");
        assertEquals(
                "http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld",
                n.at("/@context").asText());
        assertEquals("textSigle", n.at("/corpus/key").asText());
        assertEquals(2, n.at("/corpus/value").size());

        n = testRetrieveField("system", "named-vc1", "author");
        assertEquals(2, n.at("/corpus/value").size());
        assertEquals("Goethe, Johann Wolfgang von",
                n.at("/corpus/value/0").asText());

        testRetrieveUnknownTokens();
        testRetrieveProhibitedField("system", "named-vc1", "tokens");
        testRetrieveProhibitedField("system", "named-vc1", "base");

        VirtualCorpusCache.delete("named-vc1");
    }

    private void testRetrieveUnknownTokens () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode n = testRetrieveField("system", "named-vc1", "unknown");
        assertEquals("unknown", n.at("/corpus/key").asText());
        assertEquals(0, n.at("/corpus/value").size());
    }

    @Test
    public void testRetrieveTextSigleNamedVC2 ()
            throws IOException, QueryException, KustvaktException {
        vcLoader.loadVCToCache("named-vc2", "/vc/named-vc2.jsonld");

        JsonNode n = testRetrieveField("system", "named-vc2", "textSigle");
        assertEquals(2, n.at("/corpus/value").size());
        VirtualCorpusCache.delete("named-vc2");
    }

    @Test
    public void testRetrieveTextSigleNamedVC3 ()
            throws IOException, QueryException, KustvaktException {
        vcLoader.loadVCToCache("named-vc3", "/vc/named-vc3.jsonld");

        JsonNode n = testRetrieveField("system", "named-vc3", "textSigle");
        n = n.at("/corpus/value");
        assertEquals(1, n.size());
        assertEquals("GOE/AGI/00000", n.get(0).asText());

        VirtualCorpusCache.delete("named-vc3");
    }
}
