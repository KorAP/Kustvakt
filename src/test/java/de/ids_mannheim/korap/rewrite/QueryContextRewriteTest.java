package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;

public class QueryContextRewriteTest extends SpringJerseyTest {
    
    @Autowired
    public RewriteHandler handler;
    
    @Autowired
    private KustvaktConfiguration config;

    @Test
    public void testCutTokenContext () throws KustvaktException, Exception {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne")
                .queryParam("ql", "poliqarp")
                .queryParam("context", "60-token,60-token")
                .request()
                .get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        
        JsonNode context = node.at("/meta/context");
        assertEquals(config.getMaxTokenContext(), context.at("/left/1").asInt());
        assertEquals(config.getMaxTokenContext(), context.at("/right/1").asInt());
        
        // match context
        context = node.at("/matches/0/context");
        assertEquals(config.getMaxTokenContext(), context.at("/left/1").asInt());
        assertEquals(config.getMaxTokenContext(), context.at("/right/1").asInt());
    }

    @Test
    public void testMetaRewrite () throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("Schnee within s", "poliqarp");
        
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.setSpanContext("60-token,60-token");
        s.setMeta(meta.raw());
        
        String jsonQuery = s.toJSON();
        JsonNode queryNode = JsonUtils.readTree(jsonQuery);
        
        JsonNode context = queryNode.at("/meta/context");
        assertEquals(60, context.at("/left/1").asInt());
        assertEquals(60, context.at("/right/1").asInt());
        
        String result = handler.processQuery(s.toJSON(), new KorAPUser("test"));
        JsonNode node = JsonUtils.readTree(result);
        
        context = node.at("/meta/context");
        assertEquals(40, context.at("/left/1").asInt());
        assertEquals(40, context.at("/right/1").asInt());
        
        assertEquals("koral:rewrite", context.at("/rewrites/0/@type").asText());
        assertEquals("Kustvakt", context.at("/rewrites/0/origin").asText());
        assertEquals("operation:override", context.at("/rewrites/0/operation").asText());
        assertEquals("left", context.at("/rewrites/0/scope").asText());
        assertEquals("token", context.at("/rewrites/0/source/0").asText());
        assertEquals(60, context.at("/rewrites/0/source/1").asInt());
        
        assertEquals("right", context.at("/rewrites/1/scope").asText());
        assertEquals("token", context.at("/rewrites/1/source/0").asText());
        assertEquals(60, context.at("/rewrites/1/source/1").asInt());
        
    }
}
