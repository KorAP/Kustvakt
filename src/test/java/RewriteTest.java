import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resource.LayerMapper;
import de.ids_mannheim.korap.resource.RewriteProcessor;
import de.ids_mannheim.korap.utils.CollectionQueryBuilder3;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 18/06/2015
 */
public class RewriteTest {

    private static String simple_add_query = "[pos=ADJA]";
    private static String simple_rewrite_query = "[base=Haus]";
    private static String complex_rewrite_query = "<c=INFC>";
    private static String complex_rewrite_query2 = "[orth=laufe/i & base!=Lauf]";

    public RewriteTest() {

    }

    @BeforeClass
    public static void init() {
        BeanConfiguration.loadFileContext(
                "/Users/hanl/Projects/KorAP-project/KorAP-modules/Kustvakt-core/src/main/resources/default-config.xml");
    }

    @Test
    public void testQuery() {
        QuerySerializer s = new QuerySerializer();
        s.setQuery(simple_add_query, "poliqarp");
        System.out.println("query " + s.toJSON());

        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.add("textClass=politik & corpusID=WPD");
        System.out.println("collection query " + b.toJSON());
    }

    @Test
    public void testLayerMapper() {
        LayerMapper m = new LayerMapper();
        System.out.println("foundry " + m.findFoundry("lemma"));
        System.out.println("foundry " + m.findFoundry("surface"));
        System.out.println("foundry " + m.findFoundry("pos"));
    }

    @Test
    public void testRewrite() {
        RewriteProcessor processor = new RewriteProcessor();

        QuerySerializer s = new QuerySerializer();
        s.setQuery(complex_rewrite_query2, "poliqarp");
        System.out.println("query " + s.toJSON());
        System.out.println("finished node " + processor.process(s.toJSON()));
    }
}
