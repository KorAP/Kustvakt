package de.ids_mannheim.korap.resource.rewrite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.TestVariables;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.DocumentDao;
import de.ids_mannheim.korap.resources.Document;
import de.ids_mannheim.korap.utils.JsonUtils;
import net.sf.ehcache.CacheManager;

/**
 * @author hanl
 * @date 12/11/2015
 */
public class ResultRewriteTest extends BeanConfigTest {

    @Override
    public void initMethod () throws KustvaktException {

    }


    // otherwise cache will maintain values not relevant for other tests
    @Before
    public void before () {
        CacheManager.getInstance().getCache("documents").removeAll();
        DocumentDao dao = new DocumentDao(helper().getContext()
                .getPersistenceClient());
        dao.truncate();
    }


    @Test
    public void testPostRewriteNothingToDo () throws KustvaktException {
        RewriteHandler ha = new RewriteHandler();
        ha.insertBeans(helper().getContext());
        assertEquals("Handler could not be added to rewrite handler instance!",
                true, ha.add(DocMatchRewrite.class));

        DocumentDao dao = new DocumentDao(helper().getContext()
                .getPersistenceClient());
        Document d = dao.findbyId("BRZ13_APR.00014", null);
        assertNull(d);
        String v = ha.processResult(TestVariables.RESULT, null);
        assertEquals("results do not match",
                JsonUtils.readTree(TestVariables.RESULT), JsonUtils.readTree(v));
    }


    @Test
    public void testResultRewriteRemoveDoc () throws KustvaktException {
        DocumentDao dao = new DocumentDao(helper().getContext()
                .getPersistenceClient());

        Document doc = new Document("WPD_AAA.02439");
        doc.setDisabled(true);
        dao.storeResource(doc, null);

        RewriteHandler ha = new RewriteHandler();
        ha.insertBeans(helper().getContext());
        assertEquals("Handler could not be added to rewrite handler instance!",
                true, ha.add(DocMatchRewrite.class));

        JsonNode check = JsonUtils.readTree(TestVariables.RESULT);
        assertNotNull(check);
        int check_size = check.at("/matches").size();

        String v = ha.processResult(TestVariables.RESULT, null);
        JsonNode node = JsonUtils.readTree(v);

        assertNotNull(node);
        int size = node.at("/matches").size();
        assertNotEquals("documents were not removed", check_size, size);
        assertEquals("result does not contain required matches", 22, size);

        dao.deleteResource(doc.getPersistentID(), null);
        Document d = dao.findbyId(doc.getPersistentID(), null);
        assertNull("document should not exist anymore!", d);
    }

}
