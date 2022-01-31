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
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VirtualCorpusTextSigleTest extends VirtualCorpusTestBase {

    @Autowired
    private NamedVCLoader vcLoader;

    private JsonNode testRetrieveTextSigles (String username, String vcName)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("vc")
                .path("textSigle").path("~" + username).path(vcName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    @Test
    public void testRetriveTextSigleNamedVC1 ()
            throws IOException, QueryException, KustvaktException {
        vcLoader.loadVCToCache("named-vc1", "/vc/named-vc1.jsonld");

        JsonNode n = testRetrieveTextSigles("system", "named-vc1");
        assertEquals(
                "http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld",
                n.at("/@context").asText());
        assertEquals(2, n.at("/collection/value").size());
        VirtualCorpusCache.delete("named-vc1");
    }

    @Test
    public void testRetriveTextSigleNamedVC2 ()
            throws IOException, QueryException, KustvaktException {
        vcLoader.loadVCToCache("named-vc2", "/vc/named-vc2.jsonld");

        JsonNode n = testRetrieveTextSigles("system", "named-vc2");
        assertEquals(2, n.at("/collection/value").size());
        VirtualCorpusCache.delete("named-vc2");
    }

    @Test
    public void testRetriveTextSigleNamedVC3 ()
            throws IOException, QueryException, KustvaktException {
        vcLoader.loadVCToCache("named-vc3", "/vc/named-vc3.jsonld");

        JsonNode n = testRetrieveTextSigles("system", "named-vc3");
        n = n.at("/collection/value");
        assertEquals(1, n.size());
        assertEquals("GOE/AGI/00000", n.get(0).asText());

        VirtualCorpusCache.delete("named-vc3");
    }
}
