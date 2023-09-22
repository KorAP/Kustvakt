package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
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
    public void testPostRewriteNothingToDo() throws KustvaktException {
        assertEquals(true, ha.add(CollectionRewrite.class), "Handler could not be added to rewrite handler instance!");
        String v = ha.processResult(TestVariables.RESULT, null);
        assertEquals(JsonUtils.readTree(TestVariables.RESULT), JsonUtils.readTree(v), "results do not match");
    }
}
