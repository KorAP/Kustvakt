package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 21/10/2015
 */
public class RewriteHandlerTest {

    @BeforeClass
    public static void setup() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
        TestHelper.setupAccount();
    }

    //    @AfterClass
    public static void close() {
        TestHelper.dropUser();
        BeanConfiguration.closeApplication();
    }

    @Test
    public void initHandler() {
        RewriteHandler handler = new RewriteHandler(null);
        handler.add(FoundryInject.class);
    }

    @Test
    public void testRewriteTaskAdd() {
        RewriteHandler handler = new RewriteHandler(null);
        assert handler.add(FoundryInject.class);
        assert handler.add(DocMatchRewrite.class);
        assert handler.add(CollectionCleanupFilter.class);
        assert handler.add(IdWriter.class);
    }

    @Test
    public void testRewriteFoundryInjectLemma() {
        KustvaktConfiguration c = BeanConfiguration.getBeans()
                .getConfiguration();
        RewriteHandler handler = new RewriteHandler(c);
        handler.add(FoundryInject.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        String result = handler.preProcess(s.toJSON(), TestHelper.getUser());
        JsonNode node = JsonUtils.readTree(result);
        assert node != null;
        assert node.at("/query/wrap/layer").asText().equals("lemma");
        assert node.at("/query/wrap/foundry").asText().equals("test_l");
    }

    @Test
    public void testRewriteFoundryInjectPOS() {
        KustvaktConfiguration c = BeanConfiguration.getBeans()
                .getConfiguration();
        RewriteHandler handler = new RewriteHandler(c);
        handler.add(FoundryInject.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[pos=ADJA]", "poliqarp");
        String result = handler.preProcess(s.toJSON(), TestHelper.getUser());
        JsonNode node = JsonUtils.readTree(result);
        assert node != null;
        assert node.at("/query/wrap/layer").asText().equals("pos");
        assert node.at("/query/wrap/foundry").asText().equals("test_p");
    }

}
