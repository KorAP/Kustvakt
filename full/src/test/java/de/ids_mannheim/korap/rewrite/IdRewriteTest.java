package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.rewrite.IdWriter;
import de.ids_mannheim.korap.rewrite.RewriteHandler;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author hanl
 * @date 21/10/2015
 */
@DisplayName("Id Rewrite Test")
class IdRewriteTest extends SpringJerseyTest {

    @Autowired
    private KustvaktConfiguration config;

    @Test
    @DisplayName("Insert Token Id")
    void insertTokenId() throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        assertTrue(handler.add(IdWriter.class));
        String query = "[surface=Wort]";
        QuerySerializer s = new QuerySerializer();
        s.setQuery(query, "poliqarp");
        String value = handler.processQuery(s.toJSON(), new KorAPUser());
        JsonNode result = JsonUtils.readTree(value);
        assertNotNull(result);
        assertTrue(result.path("query").has("idn"));
    }

    @Test
    @DisplayName("Test Id Writer Test")
    void testIdWriterTest() throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        assertTrue(handler.add(IdWriter.class));
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        String result = handler.processQuery(s.toJSON(), new KorAPUser());
        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertFalse(node.at("/query/wrap").isMissingNode());
        assertFalse(node.at("/query/idn").isMissingNode());
    }
}
