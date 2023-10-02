package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
public class QueryRewriteTest extends SpringJerseyTest {

    @Test
    public void testRewriteRefNotFound() throws KustvaktException, Exception {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "{q}").queryParam("ql", "poliqarp").resolveTemplate("q", "[orth=der]{#examplequery} Baum").request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/errors/0/1").asText(), "Query system/examplequery is not found.");
    }

    @Test
    public void testRewriteSystemQuery() throws KustvaktException, Exception {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "{q}").queryParam("ql", "poliqarp").resolveTemplate("q", "[orth=der]{#system-q} Baum").request().get();
        String ent = response.readEntity(String.class);
        // System.out.println(ent);
        JsonNode node = JsonUtils.readTree(ent);
    }

    @Test
    public void testRewriteRefRewrite() throws KustvaktException, Exception {
        // Added in the database migration sql for tests
        Response response = target().path(API_VERSION).path("search").queryParam("q", "{q}").queryParam("ql", "poliqarp").resolveTemplate("q", "[orth=der]{#dory/dory-q} Baum").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("dory", "pass")).get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/query/operands/1/@type").asText(), "koral:token");
        assertEquals(node.at("/query/operands/1/rewrites/0/scope").asText(), "@type(koral:queryRef)");
    }
}
