import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resource.LayerMapper;
import de.ids_mannheim.korap.resource.rewrite.FoundryInject;
import de.ids_mannheim.korap.resource.rewrite.RewriteHandler;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 18/06/2015
 */
//todo: refactor and structure tests
public class FoundryRewriteTest {

    private static String simple_add_query = "[pos=ADJA]";
    private static String simple_rewrite_query = "[base=Haus]";
    private static String complex_rewrite_query = "<c=INFC>";
    private static String complex_rewrite_query2 = "[orth=laufe/i & base!=Lauf]";
    private static String complex_rewrite_query3 = "[(base=laufen | base=gehen) & tt/pos=VVFIN]";

    private static KustvaktConfiguration config;

    @BeforeClass
    public static void init() {
        BeanConfiguration.loadClasspathContext();
        config = BeanConfiguration.getBeans().getConfiguration();
    }

    @Test
    public void testSimpleFoundryAddThrowsNoError() {
        RewriteHandler processor = new RewriteHandler();
        processor.add(new FoundryInject(config));
        QuerySerializer s = new QuerySerializer();
        s.setQuery(simple_add_query, "poliqarp");
        String result = processor.apply(s.toJSON(), null);
        JsonNode node = JsonUtils.readTree(result);

        assert node != null;
        assert !node.at("/query/wrap/foundry").isMissingNode();
    }

    @Test
    public void testDefaultLayerMapperThrowsNoException() {
        LayerMapper m = new LayerMapper(config);

        assert m.findFoundry("lemma").equals(config.getDefault_lemma());
        assert m.findFoundry("pos").equals(config.getDefault_pos());
        assert m.findFoundry("surface").equals(config.getDefault_token());
        assert m.findFoundry("d").equals(config.getDefault_dep());
        assert m.findFoundry("c").equals(config.getDefault_const());
    }

    @Test
    public void testFoundryInjectPosNoErrors() {
        QuerySerializer s = new QuerySerializer();
        RewriteHandler handler = new RewriteHandler();
        s.setQuery("[pos=ADJA]", "poliqarp");
        handler.add(new FoundryInject(config));
        String result = handler.apply(s.toJSON(), null);
        JsonNode node = JsonUtils.readTree(result);

        assert node != null;
        assert !node.at("/query/wrap/foundry").isMissingNode();
        assert !node.at("/query/wrap/rewrites").isMissingNode();
        assert node.at("/query/wrap/rewrites/0/@type").asText()
                .equals("koral:rewrite");
    }

    @Test
    public void testFoundryInjectJoinedQueryNoErrors() {
        QuerySerializer s = new QuerySerializer();
        RewriteHandler handler = new RewriteHandler();
        s.setQuery("[orth=laufe/i & base!=Lauf]", "poliqarp");
        handler.add(new FoundryInject(config));
        String result = handler.apply(s.toJSON(), null);
        JsonNode node = JsonUtils.readTree(result);

        assert node != null;
        assert node.at("/query/wrap/@type").asText().equals("koral:termGroup");
        assert !node.at("/query/wrap/operands/0/foundry").isMissingNode();
        assert !node.at("/query/wrap/operands/0/rewrites").isMissingNode();
        assert !node.at("/query/wrap/operands/1/foundry").isMissingNode();
        assert !node.at("/query/wrap/operands/1/rewrites").isMissingNode();
    }

    @Test
    public void testFoundryInjectGroupedQueryNoErrors() {
        QuerySerializer s = new QuerySerializer();
        RewriteHandler handler = new RewriteHandler();
        s.setQuery("[(base=laufen | base=gehen) & tt/pos=VVFIN]", "poliqarp");
        handler.add(new FoundryInject(config));
        String result = handler.apply(s.toJSON(), null);
        JsonNode node = JsonUtils.readTree(result);

        assert node != null;
        assert node.at("/query/wrap/@type").asText().equals("koral:termGroup");
        assert !node.at("/query/wrap/operands/0/operands/0/foundry")
                .isMissingNode();
        assert !node.at("/query/wrap/operands/0/operands/0/rewrites")
                .isMissingNode();
        assert !node.at("/query/wrap/operands/0/operands/1/foundry")
                .isMissingNode();
        assert !node.at("/query/wrap/operands/0/operands/1/rewrites")
                .isMissingNode();

        assert !node.at("/query/wrap/operands/1/foundry").isMissingNode();
        assert node.at("/query/wrap/operands/1/rewrites").isMissingNode();
    }

}
