package de.ids_mannheim.korap.resource.rewrite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.TestVariables;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.DocumentDao;
import de.ids_mannheim.korap.resources.Document;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;

/** EM: DocumentDao is not available anymore.
 * Created by hanl on 30.05.16.
 */
@Deprecated
@Ignore
public class RewriteBenchmarkTest extends BeanConfigTest {


    private static int THRESHOLD = 1000;


    @Test
    public void testDocMatchRewriteByTextSigle () throws KustvaktException {
        DocumentDao dao = new DocumentDao(helper().getContext()
                .getPersistenceClient());

        int i = 999;
        for (int j = 100; j < i; j++) {
            Document doc = new Document("WPD_AAA.02" + j);
            doc.setDisabled(true);
            dao.storeResource(doc, null);
        }
        RewriteHandler ha = new RewriteHandler();
        ha.insertBeans(helper().getContext());
        assertEquals("Handler could not be added to rewrite handler instance!",
                true, ha.add(DocMatchRewrite.class));

        DateTime now = TimeUtils.getNow();
        String v = ha.processResult(TestVariables.RESULT, null);
        long diff = TimeUtils.calcDiff(now, new DateTime());
        assertTrue(diff < THRESHOLD);
        JsonNode node = JsonUtils.readTree(v);

        JsonNode check = JsonUtils.readTree(TestVariables.RESULT);
        assertNotNull(check);
        int check_size = check.at("/matches").size();

        assertNotNull(node);
        int size = node.at("/matches").size();
        assertNotEquals("documents were not removed", check_size, size);

        dao.truncate();
    }


    @Test
    public void testDocMatchRewriteByDocSigle () throws KustvaktException {
        DocumentDao dao = new DocumentDao(helper().getContext()
                .getPersistenceClient());

        Document doc = new Document("WPD_AAA");
        doc.setDisabled(true);
        dao.storeResource(doc, null);

        RewriteHandler ha = new RewriteHandler();
        ha.insertBeans(helper().getContext());
        assertEquals("Handler could not be added to rewrite handler instance!",
                true, ha.add(DocMatchRewrite.class));

        DateTime now = TimeUtils.getNow();
        String v = ha.processResult(TestVariables.RESULT, null);
        long diff = TimeUtils.calcDiff(now, new DateTime());
        assertTrue(diff < THRESHOLD);
        JsonNode node = JsonUtils.readTree(v);

        JsonNode check = JsonUtils.readTree(TestVariables.RESULT);
        assertNotNull(check);
        int check_size = check.at("/matches").size();

        assertNotNull(node);
        int size = node.at("/matches").size();
        assertNotEquals("documents were not removed", check_size, size);
        assertEquals(0, size);
        dao.truncate();
    }


    @Test
    public void testCollectionRewriteInject () {

    }


    @Test
    public void testCollectionRewriteRemoval () {

    }


    @Override
    public void initMethod () throws KustvaktException {}
}
