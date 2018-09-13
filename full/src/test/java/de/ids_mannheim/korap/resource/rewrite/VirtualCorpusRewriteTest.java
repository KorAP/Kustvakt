package de.ids_mannheim.korap.resource.rewrite;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 *
 */
public class VirtualCorpusRewriteTest extends SpringJerseyTest {

    @Test
    public void testSearchFreeWithVCRef () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system VC\"")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");

        assertEquals("koral:docGroup", node.at("/@type").asText());
        assertEquals(3, node.at("/operands/1/rewrites").size());
        node = node.at("/operands/1/rewrites");
        assertEquals("operation:deletion", node.at("/0/operation").asText());
        assertEquals("operation:deletion", node.at("/1/operation").asText());
        assertEquals("operation:insertion", node.at("/2/operation").asText());
    }

    @Test
    public void testSearchPubWithVCRef () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"system VC\"")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("user", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/collection");
        assertEquals("koral:docGroup", node.at("/@type").asText());
        assertEquals("koral:docGroup", node.at("/operands/0/@type").asText());
        assertEquals(3, node.at("/operands/1/rewrites").size());
        assertEquals("koral:doc", node.at("/operands/1/@type").asText());
        assertEquals("GOE", node.at("/operands/1/value").asText());
        assertEquals("corpusSigle", node.at("/operands/1/key").asText());
    }
}
