package de.ids_mannheim.korap.rewrite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl, margaretha
 * @date 18/06/2015
 */
//MH todo: check position and information of rewrites!
public class FoundryRewriteTest extends SpringJerseyTest {

//    private static String simple_add_query = "[pos=ADJA]";
//    private static String simple_rewrite_query = "[base=Haus]";
//    private static String complex_rewrite_query = "<c=INFC>";
//    private static String complex_rewrite_query2 = "[orth=laufe/i & base!=Lauf]";
//    private static String complex_rewrite_query3 = "[(base=laufen | base=gehen) & tt/pos=VVFIN]";

    @Autowired
    public KustvaktConfiguration config;
    @Autowired
    public RewriteHandler handler; 
    @Autowired
    private LayerMapper m;
    
    @Test
    public void testSearchRewriteFoundryWithUserSetting () throws KustvaktException {
        // create user setting
        String json = "{\"pos-foundry\":\"opennlp\"}";
        String username = "foundryRewriteTest";
        String pathUsername = "~" + username;
        ClientResponse response = resource().path(API_VERSION)
                .path(pathUsername).path("setting")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .type(MediaType.APPLICATION_JSON).entity(json)
                .put(ClientResponse.class);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        // search
        response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[pos=ADJA]").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals("opennlp", node.at("/query/wrap/foundry").asText());
        assertEquals("foundry",
                node.at("/query/wrap/rewrites/0/scope").asText());

    }

    @Test
    public void testRewritePosFoundryWithUserSetting ()
            throws KustvaktException {
        // EM: see
        // full/src/main/resources/db/insert/V3.6__insert_default_settings.sql

        String username = "bubbles";
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[pos=ADJA]", "poliqarp");
        String result =
                handler.processQuery(s.toJSON(), new KorAPUser(username));
        JsonNode node = JsonUtils.readTree(result);
        assertEquals("corenlp", node.at("/query/wrap/foundry").asText());
        assertEquals("foundry",
                node.at("/query/wrap/rewrites/0/scope").asText());

    }

    @Test
    public void testRewriteLemmaFoundryWithUserSetting ()
            throws KustvaktException {
        String username = "bubbles";
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        String result =
                handler.processQuery(s.toJSON(), new KorAPUser(username));
        JsonNode node = JsonUtils.readTree(result);
        // EM: only for testing, in fact, opennlp lemma does not
        // exist!
        assertEquals("opennlp", node.at("/query/wrap/foundry").asText());
        assertEquals("foundry",
                node.at("/query/wrap/rewrites/0/scope").asText());
    }
    
    
    @Test
    public void testDefaultLayerMapperThrowsNoException () {
        assertEquals(config.getDefault_lemma(), m.findFoundry("lemma"));
        assertEquals(config.getDefault_pos(), m.findFoundry("pos"));
        assertEquals(config.getDefault_token(), m.findFoundry("surface"));
        assertEquals(config.getDefault_dep(), m.findFoundry("d"));
        assertEquals(config.getDefault_const(), m.findFoundry("c"));
    }

    @Test
    public void testDefaultFoundryInjectLemmaThrowsNoError ()
            throws KustvaktException {

        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        String result = handler.processQuery(s.toJSON(), new KorAPUser("test"));
        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertFalse(node.at("/query/wrap/foundry").isMissingNode());
        assertEquals(config.getDefault_lemma(), node.at("/query/wrap/foundry")
                .asText());
        assertEquals("lemma", node.at("/query/wrap/layer").asText());
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
        assertEquals("koral:rewrite", node.at("/query/wrap/rewrites/0/@type")
                .asText());
    }


    @Test
    public void testDefaultFoundryInjectPOSNoErrors () throws KustvaktException {

        QuerySerializer s = new QuerySerializer();
        s.setQuery("[pos=ADJA]", "poliqarp");
        String result = handler.processQuery(s.toJSON(), new KorAPUser("test"));
        JsonNode node = JsonUtils.readTree(result);

        assertNotNull(node);
        assertFalse(node.at("/query/wrap/foundry").isMissingNode());
        assertEquals(config.getDefault_pos(), node.at("/query/wrap/foundry")
                .asText());
        assertEquals("pos", node.at("/query/wrap/layer").asText());
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
        assertEquals("koral:rewrite", node.at("/query/wrap/rewrites/0/@type")
                .asText());

    }
    
    @Test
    public void testFoundryInjectJoinedQueryNoErrors ()
            throws KustvaktException {

        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=laufe/i & base!=Lauf]", "poliqarp");
        String result = handler.processQuery(s.toJSON(), new KorAPUser("test"));
        JsonNode node = JsonUtils.readTree(result);

        assertNotNull(node);
        assertEquals("koral:termGroup", node.at("/query/wrap/@type").asText());
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
        String result = handler.processQuery(s.toJSON(), new KorAPUser("test"));
        JsonNode node = JsonUtils.readTree(result);

        assertNotNull(node);
        assertEquals("koral:termGroup", node.at("/query/wrap/@type").asText());
        assertFalse(node.at("/query/wrap/operands/0/foundry")
                .isMissingNode());
        assertFalse(node.at("/query/wrap/operands/0/rewrites")
                .isMissingNode());
        assertFalse(node.at("/query/wrap/operands/1/foundry").isMissingNode());
        assertTrue(node.at("/query/wrap/operands/1/rewrites").isMissingNode());
    }

    @Test
    public void testFoundryBaseRewrite() throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=laufen]", "poliqarp");
        String result = handler.processQuery(s.toJSON(), new KorAPUser("test"));
        JsonNode node = JsonUtils.readTree(result);

        assertEquals("koral:term", node.at("/query/wrap/@type").asText());
        assertFalse(node.at("/query/wrap/foundry")
                .isMissingNode());
        assertFalse(node.at("/query/wrap/rewrites")
                .isMissingNode());
    }

}
