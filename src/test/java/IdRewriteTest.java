import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resource.rewrite.IdWriter;
import de.ids_mannheim.korap.resource.rewrite.RewriteHandler;
import de.ids_mannheim.korap.utils.JsonUtils;
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
    public static void close() {
        BeanConfiguration.closeApplication();
    }

    @Test
    public void insertTokenId() {
        RewriteHandler handler = new RewriteHandler(
                BeanConfiguration.getBeans().getConfiguration());
        assert handler.add(IdWriter.class);

        String query = "[surface=Wort]";
        QuerySerializer s = new QuerySerializer();
        s.setQuery(query, "poliqarp");

        String value = handler.preProcess(s.toJSON(), null);
        JsonNode result = JsonUtils.readTree(value);

        assert result != null;
        assert result.path("query").has("idn");

    }

}
