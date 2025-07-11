package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.TestBase;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @author hanl, margaretha
 * @date 18/06/2015
 */
// MH todo: check position and information of rewrites!
public class FoundryRewriteTest extends TestBase {

    // private static String simple_add_query = "[pos=ADJA]";
    // private static String simple_rewrite_query = "[base=Haus]";
    // private static String complex_rewrite_query = "<c=INFC>";
    // private static String complex_rewrite_query2 = "[orth=laufe/i & base!=Lauf]";
    // private static String complex_rewrite_query3 = "[(base=laufen | base=gehen) & tt/pos=VVFIN]";
    @Autowired
    public KustvaktConfiguration config;

    @Autowired
    public RewriteHandler rewriteHandler;

    @Autowired
    private LayerMapper m;

    @Test
    public void testSearchRewriteFoundryWithUserSetting ()
            throws KustvaktException {
        // create user setting
        String json = "{\"pos-foundry\":\"opennlp\"}";
        String username = "foundryRewriteTest";
        String pathUsername = "~" + username;
        Response response = target().path(API_VERSION).path(pathUsername)
                .path("setting").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .put(Entity.json(json));
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        // search
        response = target().path(API_VERSION).path("search")
                .queryParam("q", "[pos=ADJA]").queryParam("ql", "poliqarp")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .accept(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/query/wrap/foundry").asText(), "opennlp");
        assertEquals(node.at("/query/wrap/rewrites/0/scope").asText(),
                "foundry");
    }

    @Test
    public void testRewritePosFoundryWithUserSetting ()
            throws KustvaktException {
		Map<String, Object> map = new HashMap<>();
		map.put("pos-foundry", "corenlp");
		map.put("lemma-foundry", "opennlp");
		Response response = createUpdateDefaultSettings("bubbles", map);
		assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        String username = "bubbles";
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[pos=ADJA]", "poliqarp");
        String result = rewriteHandler.processQuery(s.toJSON(),
                new KorAPUser(username));
        JsonNode node = JsonUtils.readTree(result);
        assertEquals(node.at("/query/wrap/foundry").asText(), "corenlp");
        assertEquals(node.at("/query/wrap/rewrites/0/scope").asText(),
                "foundry");
        
        testRewriteLemmaFoundryWithUserSetting(username);
        testDeleteSetting(username);
    }

    private void testRewriteLemmaFoundryWithUserSetting (String username)
            throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        String result = rewriteHandler.processQuery(s.toJSON(),
                new KorAPUser(username));
        JsonNode node = JsonUtils.readTree(result);
        // EM: only for testing, in fact, opennlp lemma does not
        // exist!
        assertEquals(node.at("/query/wrap/foundry").asText(), "opennlp");
        assertEquals(node.at("/query/wrap/rewrites/0/scope").asText(),
                "foundry");
    }

    @Test
    public void testDefaultLayerMapperThrowsNoException () {
        assertEquals(config.getDefault_lemma(), m.findFoundry("lemma"));
        assertEquals(config.getDefault_pos(), m.findFoundry("pos"));
        assertEquals(config.getDefault_orthography(), m.findFoundry("surface"));
        assertEquals(config.getDefault_dep(), m.findFoundry("d"));
        assertEquals(config.getDefault_const(), m.findFoundry("c"));
    }

    @Test
    public void testDefaultFoundryInjectLemmaThrowsNoError ()
            throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        String result = rewriteHandler.processQuery(s.toJSON(), new KorAPUser("test"));
        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertFalse(node.at("/query/wrap/foundry").isMissingNode());
        assertEquals(config.getDefault_lemma(),
                node.at("/query/wrap/foundry").asText());
        assertEquals(node.at("/query/wrap/layer").asText(), "lemma");
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
        assertEquals(node.at("/query/wrap/rewrites/0/@type").asText(),
                "koral:rewrite");
    }

    @Test
    public void testDefaultFoundryInjectPOSNoErrors ()
            throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[pos=ADJA]", "poliqarp");
        String result = rewriteHandler.processQuery(s.toJSON(), new KorAPUser("test"));
        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertFalse(node.at("/query/wrap/foundry").isMissingNode());
        assertEquals(config.getDefault_pos(),
                node.at("/query/wrap/foundry").asText());
        assertEquals(node.at("/query/wrap/layer").asText(), "pos");
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
        assertEquals(node.at("/query/wrap/rewrites/0/@type").asText(),
                "koral:rewrite");
    }

    @Test
    public void testFoundryInjectJoinedQueryNoErrors ()
            throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=laufe/i & base!=Lauf]", "poliqarp");
        String result = rewriteHandler.processQuery(s.toJSON(), new KorAPUser("test"));
        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertEquals(node.at("/query/wrap/@type").asText(), "koral:termGroup");
        assertFalse(node.at("/query/wrap/operands/0/foundry").isMissingNode());
        assertFalse(node.at("/query/wrap/operands/0/rewrites").isMissingNode());
        assertFalse(node.at("/query/wrap/operands/1/foundry").isMissingNode());
        assertFalse(node.at("/query/wrap/operands/1/rewrites").isMissingNode());
    }

    @Test
    public void testFoundryInjectGroupedQueryNoErrors ()
            throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[(base=laufen | tt/pos=VVFIN)]", "poliqarp");
        String result = rewriteHandler.processQuery(s.toJSON(), new KorAPUser("test"));
        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertEquals(node.at("/query/wrap/@type").asText(), "koral:termGroup");
        assertFalse(node.at("/query/wrap/operands/0/foundry").isMissingNode());
        assertFalse(node.at("/query/wrap/operands/0/rewrites").isMissingNode());
        assertFalse(node.at("/query/wrap/operands/1/foundry").isMissingNode());
        assertTrue(node.at("/query/wrap/operands/1/rewrites").isMissingNode());
    }

    @Test
    public void testFoundryBaseRewrite () throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=laufen]", "poliqarp");
        String result = rewriteHandler.processQuery(s.toJSON(), new KorAPUser("test"));
        JsonNode node = JsonUtils.readTree(result);
        assertEquals(node.at("/query/wrap/@type").asText(), "koral:term");
        assertFalse(node.at("/query/wrap/foundry").isMissingNode());
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
    }
}
