package de.ids_mannheim.korap.rewrite;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author diewald
 *
 */
public class QueryRewriteTest extends SpringJerseyTest {

    @Test
    public void testRewriteRefNotFound ()
            throws KustvaktException, Exception {

        ClientResponse response = resource().path(API_VERSION).path("search")
            .queryParam("q", "[orth=der]{%23examplequery} Baum")
            .queryParam("ql", "poliqarp")
            .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals("Query system/examplequery is not found.",
                node.at("/errors/0/1").asText());
    }
    
    @Test
    public void testRewriteSystemQuery ()
            throws KustvaktException, Exception {

        ClientResponse response = resource().path(API_VERSION).path("search")
            .queryParam("q", "[orth=der]{%23system-q} Baum")
            .queryParam("ql", "poliqarp")
            .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        System.out.println(ent);
        JsonNode node = JsonUtils.readTree(ent);
    }

    @Test
    public void testRewriteRefRewrite ()
            throws KustvaktException, Exception {

        // Added in the database migration sql for tests
        ClientResponse response = resource().path(API_VERSION).path("search")
            .queryParam("q", "[orth=der]{%23dory/dory-q} Baum")
            .queryParam("ql", "poliqarp")
            .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                    .createBasicAuthorizationHeaderValue("dory", "pass"))
            .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals("koral:token", node.at("/query/operands/1/@type").asText());
        assertEquals("@type(koral:queryRef)",
                     node.at("/query/operands/1/rewrites/0/scope").asText());
    }
}
