package de.ids_mannheim.korap.resource.rewrite;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.TestVariables;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.rewrite.CollectionRewrite;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl
 * @date 12/11/2015
 */
public class ResultRewriteTest extends BeanConfigTest {

    @Override
    public void initMethod () throws KustvaktException {

    }

    @Test
    public void testPostRewriteNothingToDo () throws KustvaktException {
        RewriteHandler ha = new RewriteHandler();
        ha.insertBeans(helper().getContext());
        assertEquals("Handler could not be added to rewrite handler instance!",
                true, ha.add(CollectionRewrite.class));

        String v = ha.processResult(TestVariables.RESULT, null);
        assertEquals("results do not match",
                JsonUtils.readTree(TestVariables.RESULT), JsonUtils.readTree(v));
    }

}
