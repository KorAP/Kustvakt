package de.ids_mannheim.korap.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.SearchKrill;

/**
 * Created by hanl on 02.06.16.
 */
public class SearchKrillTest extends BeanConfigTest {

    @Override
    public void initMethod () throws KustvaktException {

    }


    @Test
    public void testInit () {
        KustvaktConfiguration config = helper().getContext().getConfiguration();
        SearchKrill krill = new SearchKrill(config.getIndexDir());
        assertNotNull(krill);
    }


    @Test
    public void testIndex () {
        KustvaktConfiguration config = helper().getContext().getConfiguration();
        SearchKrill krill = new SearchKrill(config.getIndexDir());
        assertNotNull(krill);

        KrillIndex index = krill.getIndex();
        assertNotNull(index);
    }


    @Test
    public void testDocSize () {
        KustvaktConfiguration config = helper().getContext().getConfiguration();
        SearchKrill krill = new SearchKrill(config.getIndexDir());
        assertNotNull(krill);
        assertNotEquals(0, krill.getIndex().numberOf("documents"));
    }

	@Test
    public void testMatchInfo () throws KustvaktException {
	    FullConfiguration config = helper().getContext().getConfiguration();
        SearchKrill krill = new SearchKrill(config.getIndexDir());
        assertNotNull(krill);
		String matchinfo = krill.getMatch("WPD/AAA.00002/p169-197", config.getFreeLicensePattern());
		JsonNode node = JsonUtils.readTree(matchinfo);
		assertEquals("Invalid match identifier", node.at("/errors/0/1").asText());
	}

    @Test
    public void testSearch () throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=der]", "poliqarp");

        KustvaktConfiguration config = helper().getContext().getConfiguration();
        SearchKrill krill = new SearchKrill(config.getIndexDir());
        String result = krill.search(s.toJSON());

        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertNotEquals(0, node.at("/matches").size());
    }

}
