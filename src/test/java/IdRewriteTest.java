import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.resource.rewrite.IdWriter;
import de.ids_mannheim.korap.resource.rewrite.RewriteHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 21/10/2015
 */
public class IdRewriteTest {

    @BeforeClass
    public static void setup() {
        BeanConfiguration.loadClasspathContext();
    }

    @AfterClass
    public static void drop() {
        BeanConfiguration.closeApplication();
    }

    @Test
    public void insertTokenId() {
        RewriteHandler handler = new RewriteHandler(
                BeanConfiguration.getBeans().getConfiguration());
        assert handler.add(IdWriter.class);


    }

}
