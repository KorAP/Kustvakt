import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly.web.GrizzlyWebTestContainerFactory;
import de.ids_mannheim.korap.config.BeansFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 29/07/2015
 */
@Deprecated
public class JerseyTest1 extends JerseyTest {

    public JerseyTest1 () {
        super("de.ids_mannheim.korap.web.service",
                "de.ids_mannheim.korap.web.utils");
    }


    @BeforeClass
    public static void setup () {
        BeansFactory.loadClasspathContext();
    }


    @AfterClass
    public static void close () {
        BeansFactory.closeApplication();
    }


    @Override
    protected TestContainerFactory getTestContainerFactory () {
        return new GrizzlyWebTestContainerFactory();
    }


    @Test
    public void testFieldsInSearch () {
        ClientResponse response = resource().path("v0.1/search")
                .queryParam("q", "[base=Wort]").queryParam("ql", "poliqarp")
                .get(ClientResponse.class);
        System.out.println("response " + response);
    }
}
