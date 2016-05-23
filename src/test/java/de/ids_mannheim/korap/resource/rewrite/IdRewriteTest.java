package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author hanl
 * @date 21/10/2015
 */
public class IdRewriteTest extends BeanConfigTest {

    @Test
    public void insertTokenId () {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        assertTrue(handler.add(IdWriter.class));

        String query = "[surface=Wort]";
        QuerySerializer s = new QuerySerializer();
        s.setQuery(query, "poliqarp");

        String value = handler.preProcess(s.toJSON(), null);
        JsonNode result = JsonUtils.readTree(value);

        assertNotNull(result);
        assertTrue(result.path("query").has("idn"));
    }


    @Test
    public void testIdWriterTest () {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        assertTrue(handler.add(IdWriter.class));

        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        String result = handler.preProcess(s.toJSON(), null);
        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertFalse(node.at("/query/wrap").isMissingNode());
        assertFalse(node.at("/query/idn").isMissingNode());
    }


    @Override
    public void initMethod () throws KustvaktException {

    }
}
