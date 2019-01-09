package de.ids_mannheim.korap.rewrite;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.config.TestVariables;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.rewrite.CollectionRewrite;
import de.ids_mannheim.korap.rewrite.RewriteHandler;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl
 * @date 12/11/2015
 */
public class ResultRewriteTest extends SpringJerseyTest {

    @Autowired
    public RewriteHandler ha;
    
    @Test
    public void testPostRewriteNothingToDo () throws KustvaktException {
        assertEquals("Handler could not be added to rewrite handler instance!",
                true, ha.add(CollectionRewrite.class));

        String v = ha.processResult(TestVariables.RESULT, null);
        assertEquals("results do not match",
                JsonUtils.readTree(TestVariables.RESULT), JsonUtils.readTree(v));
    }

}
