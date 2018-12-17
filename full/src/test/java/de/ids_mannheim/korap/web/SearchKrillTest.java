package de.ids_mannheim.korap.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * Created by hanl on 02.06.16.
 * 
 * Updated by margaretha
 */
public class SearchKrillTest extends SpringJerseyTest {

    @Autowired
    KustvaktConfiguration config;
    
    SearchKrill krill = null;
    
    @PostConstruct
    private void createKrill () {
        krill = new SearchKrill(config.getIndexDir());
        assertNotNull(krill);
    }

    @Test
    public void testIndex () throws KustvaktException {
        KrillIndex index = krill.getIndex();
        assertNotNull(index);
    }

    @Test
    public void testDocSize () {
        assertNotEquals(0, krill.getIndex().numberOf("documents"));
    }

    @Test
    public void testMatchInfo () throws KustvaktException {
        String matchinfo = krill.getMatch("WPD/AAA.00002/p169-197",
                config.getFreeLicensePattern());
        JsonNode node = JsonUtils.readTree(matchinfo);
        assertEquals("Invalid match identifier",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testSearch () throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=der]", "poliqarp");

        String result = krill.search(s.toJSON());

        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertNotEquals(0, node.at("/matches").size());
    }

    @Test
    public void testTimeOut () throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=der]", "poliqarp");
//        s.setQuery("node ->malt/d[func=/.*/] node", "annis");
        
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("timeout", 1);
        s.setMeta(meta);

        String query = s.toJSON();
        JsonNode node = JsonUtils.readTree(query);
        assertEquals(1, node.at("/meta/timeout").asInt());
        
        String result = krill.search(query);
        node = JsonUtils.readTree(result);
        assertEquals(true,node.at("/meta/timeExceeded").asBoolean());
    }
}
