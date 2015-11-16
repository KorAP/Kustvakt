import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resource.rewrite.CollectionCleanupFilter;
import de.ids_mannheim.korap.resource.rewrite.CollectionConstraint;
import de.ids_mannheim.korap.resource.rewrite.RewriteHandler;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author hanl
 * @date 03/09/2015
 */
// todo: 20.10.15
public class CollectionRewriteTest {

    private static String simple_add_query = "[pos=ADJA]";

    private static KustvaktConfiguration config;

    @BeforeClass
    public static void init() {
        BeanConfiguration.loadClasspathContext("test-config.xml");
        config = BeanConfiguration.getBeans().getConfiguration();
    }

    @AfterClass
    public static void close() {
        BeanConfiguration.closeApplication();
    }

    @Test
    public void test2() {
        Pattern p = Pattern.compile("([\\.\\w]+)\\((.+)\\)");
        String cl = de.ids_mannheim.korap.security.ac.SecurityManager.class
                .getCanonicalName();
        Matcher m = p.matcher(cl);
        System.out.println("FOUND SOMETHING?! " + m.find());
        while (m.find())
            System.out.println("group 1 " + m.group(1));

    }

    @Test
    public void testCollectionNodeRemoveCorpusIdNoErrors() {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(simple_add_query, "poliqarp");
        s.setCollection("textClass=politik & corpusID=WPD");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(
                handler.preProcess(result, User.UserFactory.getUser("test_user")));
        assert node != null;
        assert node.at("/collection/operands").size() == 1;
    }

    @Test
    public void testCollectionNodeRemoveAllCorpusIdNoErrors() {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(simple_add_query, "poliqarp");
        s.setCollection("corpusID=BRZ13 & corpusID=WPD");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(
                handler.preProcess(result, User.UserFactory.getUser("test_user")));
        System.out.println("RESULTING REWR NODE " + node);
        assert node != null;
        assert node.at("/collection/operands").size() == 0;
    }

    @Test
    public void testCollectionNodeRemoveGroupedCorpusIdNoErrors() {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(simple_add_query, "poliqarp");
        s.setCollection(
                "(corpusID=BRZ13 & textClass=Wissenschaft) & corpusID=WPD");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(
                handler.preProcess(result, User.UserFactory.getUser("test_user")));

        System.out.println("COLLECTION NODE " + result);
        assert node != null;
        assert node.at("/collection/operands/0/@type").asText()
                .equals("koral:docGroup");
        assert node.at("/collection/operands/0/operands/0/key").asText()
                .equals("textClass");
    }

    //fixme: will probably fail when one doc groups are being refactored
    @Test
    public void testCollectionCleanEmptyDocGroupNoErrors() {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanupFilter.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(simple_add_query, "poliqarp");
        s.setCollection(
                "(corpusID=BRZ13 & corpusID=WPD) & textClass=Wissenschaft & textClass=Sport");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(
                handler.preProcess(result, User.UserFactory.getUser("test_user")));
        assert node != null;
        assert node.at("/collection/@type").asText().equals("koral:docGroup");
        assert node.at("/collection/operands").size() == 2;
        assert node.at("/collection/operands/0/key").asText()
                .equals("textClass");
        assert node.at("/collection/operands/1/key").asText()
                .equals("textClass");
    }

    @Test
    public void testCollectionCleanMoveOneDocFromGroupUpNoErrors() {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanupFilter.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(simple_add_query, "poliqarp");
        s.setCollection("(corpusID=BRZ13 & textClass=Wissenschaft)");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(
                handler.preProcess(result, User.UserFactory.getUser("test_user")));
        assert node != null;
        assert node.at("/collection/@type").asText().equals("koral:doc");
    }

    @Test
    public void testCollectionCleanEmptyGroupAndMoveOneFromGroupUpNoErrors() {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanupFilter.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(simple_add_query, "poliqarp");
        s.setCollection(
                "(corpusID=BRZ13 & corpusID=WPD) & textClass=Wissenschaft");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(
                handler.preProcess(result, User.UserFactory.getUser("test_user")));

        assert node != null;
        assert node.at("/collection/@type").asText().equals("koral:doc");
        assert node.at("/collection/key").asText().equals("textClass");
    }

    @Test
    public void testCollectionRemoveAndMoveOneFromGroupUpNoErrors() {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanupFilter.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(simple_add_query, "poliqarp");
        s.setCollection(
                "(docID=random & textClass=Wissenschaft) & corpusID=WPD");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(
                handler.preProcess(result, User.UserFactory.getUser("test_user")));
        System.out.println("original node " + result);
        System.out.println("result node " + node);
        assert node != null;
        assert node.at("/collection/@type").asText().equals("koral:docGroup");
        assert node.at("/collection/operands").size() == 2;
    }

}
