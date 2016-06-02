package de.ids_mannheim.korap.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.SearchKrill;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by hanl on 02.06.16.
 */
public class SearchKrillTest extends BeanConfigTest {


    @Test
    public void testInit () {
        KustvaktConfiguration config = helper().getContext().getConfiguration();
        SearchKrill krill = new SearchKrill(config.getIndexDir());
        assertNotNull(krill);
    }


    @Test
    public void testDocSize () {
        KustvaktConfiguration config = helper().getContext().getConfiguration();
        SearchKrill krill = new SearchKrill(config.getIndexDir());
        assertNotNull(krill);
        assertNotEquals(0, krill.getIndex().numberOf("documents"));
    }


    @Test
    public void testSearch () {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");


        KustvaktConfiguration config = helper().getContext().getConfiguration();
        SearchKrill krill = new SearchKrill(config.getIndexDir());
        String result = krill.search(s.toJSON());

        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertNotEquals(0, node.at("/matches").size());



    }



    @Override
    public void initMethod () throws KustvaktException {

    }
}
