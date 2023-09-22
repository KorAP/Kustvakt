package de.ids_mannheim.korap.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.annotation.PostConstruct;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Created by hanl on 02.06.16.
 * <p>
 * Updated by margaretha
 */
@DisplayName("Search Krill Test")
class SearchKrillTest extends SpringJerseyTest {

    @Autowired
    KustvaktConfiguration config;

    SearchKrill krill = null;

    @PostConstruct
    private void createKrill() {
        krill = new SearchKrill(config.getIndexDir());
        assertNotNull(krill);
    }

    @Test
    @DisplayName("Test Index")
    void testIndex() throws KustvaktException {
        KrillIndex index = krill.getIndex();
        assertNotNull(index);
    }

    @Test
    @DisplayName("Test Doc Size")
    void testDocSize() {
        assertNotEquals(0, krill.getIndex().numberOf("documents"));
    }

    @Test
    @DisplayName("Test Match Info")
    void testMatchInfo() throws KustvaktException {
        String matchinfo = krill.getMatch("WPD/AAA.00002/p169-197", config.getFreeLicensePattern());
        JsonNode node = JsonUtils.readTree(matchinfo);
        assertEquals(node.at("/errors/0/1").asText(), "Invalid match identifier");
    }

    @Test
    @DisplayName("Test Search")
    void testSearch() throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=der]", "poliqarp");
        String result = krill.search(s.toJSON());
        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertNotEquals(0, node.at("/matches").size());
    }

    @Test
    @DisplayName("Test Time Out")
    void testTimeOut() throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=der]", "poliqarp");
        // s.setQuery("node ->malt/d[func=/.*/] node", "annis");
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("timeout", 1);
        s.setMeta(meta);
        String query = s.toJSON();
        JsonNode node = JsonUtils.readTree(query);
        assertEquals(1, node.at("/meta/timeout").asInt());
        String result = krill.search(query);
        node = JsonUtils.readTree(result);
        assertEquals(true, node.at("/meta/timeExceeded").asBoolean());
    }
}
