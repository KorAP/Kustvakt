package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;

public class QueryContextRewriteTest extends SpringJerseyTest {
    
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

    
}
