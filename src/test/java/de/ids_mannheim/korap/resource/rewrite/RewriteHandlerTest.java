import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.resource.rewrite.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 21/10/2015
 */
public class RewriteHandlerTest {

    @BeforeClass
    public static void setup() {
        BeanConfiguration.loadClasspathContext();
    }

    @Test
    public void initHandler() {
        RewriteHandler handler = new RewriteHandler(null);
        handler.add(FoundryInject.class);
    }

    @Test
    public void testRewriteTastAdd() {
        RewriteHandler handler = new RewriteHandler(null);
        handler.add(FoundryInject.class);
        handler.add(DocMatchRewrite.class);
        handler.add(CollectionCleanupFilter.class);
        handler.add(IdWriter.class);
    }

    @AfterClass
    public static void close() {
        BeanConfiguration.closeApplication();
    }

}
