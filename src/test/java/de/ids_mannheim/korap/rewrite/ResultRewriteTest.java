package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.config.TestVariables;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.rewrite.AvailabilityRewrite;
import de.ids_mannheim.korap.rewrite.RewriteHandler;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl
 * @date 12/11/2015
 */
public class ResultRewriteTest extends SpringJerseyTest {

    @Autowired
    public RewriteHandler rewriteHandler;

    @Test
    public void testPostRewriteNothingToDo () throws KustvaktException {
        assertEquals(true, rewriteHandler.add(AvailabilityRewrite.class),
                "Handler could not be added to rewrite handler instance!");
        String v = rewriteHandler.processResult(TestVariables.RESULT, null);
        assertEquals(JsonUtils.readTree(TestVariables.RESULT),
                JsonUtils.readTree(v), "results do not match");
    }
}
