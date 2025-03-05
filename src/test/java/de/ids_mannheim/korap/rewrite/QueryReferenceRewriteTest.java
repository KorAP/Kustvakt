package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;

/**
 * @author diewald
 */
public class QueryReferenceRewriteTest extends SpringJerseyTest {

    @Test
    public void testRewriteRefNotFound () throws KustvaktException, Exception {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "{q}").queryParam("ql", "poliqarp")
                .resolveTemplate("q", "[orth=der]{#examplequery} Baum")
                .request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals("Query system/examplequery is not found.",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testRewriteSystemQuery () throws KustvaktException, Exception {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "{q}").queryParam("ql", "poliqarp")
                .resolveTemplate("q", "[orth=der]{#system-q} Baum").request()
                .get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        node = node.at("/query/operands/1/rewrites");
        assertEquals(1, node.size());
        assertEquals("Kustvakt", node.at("/0/editor").asText());
        assertEquals("operation:override", node.at("/0/operation").asText());
        assertEquals("koral:queryRef", node.at("/0/original/@type").asText());
        assertEquals("system-q", node.at("/0/original/ref").asText());
        assertTrue(node.at("/0/scope").isMissingNode());
    }

    @Test
    public void testRewriteRefRewrite () throws KustvaktException, Exception {
        // Added in the database migration sql for tests
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "{q}").queryParam("ql", "poliqarp")
                .resolveTemplate("q", "[orth=der]{#dory/dory-q} Baum").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/query/operands/1/@type").asText(),
                "koral:token");

        node = node.at("/query/operands/1/rewrites");
        assertEquals(1, node.size());
        assertEquals("Kustvakt", node.at("/0/editor").asText());
        assertEquals("operation:override", node.at("/0/operation").asText());
        assertEquals("koral:queryRef", node.at("/0/original/@type").asText());
        assertEquals("dory/dory-q", node.at("/0/original/ref").asText());
        assertTrue(node.at("/0/scope").isMissingNode());
    }
}
