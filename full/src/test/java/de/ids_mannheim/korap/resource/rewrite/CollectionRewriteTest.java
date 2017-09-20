package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.TestVariables;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author hanl
 * @date 03/09/2015
 */
public class CollectionRewriteTest extends BeanConfigTest {

    private static KustvaktConfiguration config;


    @Override
    public void initMethod () throws KustvaktException {
//        helper().runBootInterfaces();
        config = helper().getContext().getConfiguration();
    }


    @Deprecated
    @Test
    public void test2 () {
        Pattern p = Pattern.compile("([\\.\\w]+)\\((.+)\\)");
        String cl = de.ids_mannheim.korap.security.ac.SecurityManager.class
                .getCanonicalName();
        Matcher m = p.matcher(cl);
        while (m.find())
            System.out.println("group 1 " + m.group(1));

    }


    @Test
    public void testCollectionNodeRemoveCorpusIdNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("textClass=politik & corpusSigle=WPD");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(1, node.at("/collection/operands").size());
    }


    @Test
    public void testCollectionNodeDeletionNotification () {

    }


    @Test
    public void testCollectionNodeRemoveAllCorpusIdNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("corpusSigle=BRZ13 & corpusSigle=WPD");
        String result = s.toJSON();

        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));

        assertNotNull(node);
        assertEquals(0, node.at("/collection/operands").size());
        assertEquals("koral:rewrite", node.at("/collection/rewrites/0/@type")
                .asText());
    }


    @Test
    public void testCollectionNodeRemoveGroupedCorpusIdNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(corpusSigle=BRZ13 & textClass=Wissenschaft) & corpusSigle=WPD");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));

        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/operands/0/@type")
                .asText());
        assertEquals("textClass",
                node.at("/collection/operands/0/operands/0/key").asText());
        assertEquals("koral:rewrite", node.at("/collection/rewrites/0/@type")
                .asText());
    }


    //fixme: will probably fail when one doc groups are being refactored
    @Test
    public void testCollectionCleanEmptyDocGroupNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(corpusSigle=BRZ13 & corpusSigle=WPD) & textClass=Wissenschaft & textClass=Sport");
        String result = s.toJSON();

        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));


        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals(2, node.at("/collection/operands").size());

        assertEquals("textClass", node.at("/collection/operands/0/key")
                .asText());
        assertEquals("textClass", node.at("/collection/operands/1/key")
                .asText());
        assertEquals("koral:rewrite", node.at("/collection/rewrites/0/@type")
                .asText());
    }


    @Test
    public void testCollectionCleanMoveOneDocFromGroupUpNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(corpusSigle=BRZ13 & textClass=wissenschaft)");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("textClass", node.at("/collection/key").asText());
        assertEquals("wissenschaft", node.at("/collection/value").asText());
        assertEquals("koral:rewrite", node.at("/collection/rewrites/0/@type")
                .asText());
    }


    @Test
    public void testCollectionCleanEmptyGroupAndMoveOneFromGroupUpNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(corpusSigle=BRZ13 & corpusSigle=WPD) & textClass=Wissenschaft");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));

        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("textClass", node.at("/collection/key").asText());
        assertEquals("koral:rewrite", node.at("/collection/rewrites/0/@type")
                .asText());
    }


    @Test
    public void testCollectionRemoveAndMoveOneFromGroupUpNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(docID=random & textClass=Wissenschaft) & corpusSigle=WPD");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals("koral:doc", node.at("/collection/operands/0/@type")
                .asText());
        assertEquals("koral:doc", node.at("/collection/operands/1/@type")
                .asText());
        assertEquals("koral:rewrite", node.at("/collection/rewrites/0/@type")
                .asText());
    }


    @Test
    public void testPublicCollectionRewriteEmptyAdd () throws KustvaktException {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(CollectionRewrite.class);

        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals("availability", node.at("/collection/key")
                .asText());
        assertEquals("CC-BY.*", node.at("/collection/value")
                .asText());
        assertEquals("koral:rewrite", node.at("/collection/rewrites/0/@type")
                .asText());
        assertEquals("availability(FREE)", node.at("/collection/rewrites/0/scope")
                .asText());
        //todo:
    }


    @Test
    public void testPublicCollectionRewriteNonEmptyAdd ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(CollectionRewrite.class);

        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(docSigle=WPD_AAA & textClass=wissenschaft)");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals("availability",
                node.at("/collection/operands/0/key").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("docSigle",
                node.at("/collection/operands/1/operands/0/key").asText());
        assertEquals("textClass",
                node.at("/collection/operands/1/operands/1/key").asText());
        assertEquals("koral:rewrite", node.at("/collection/rewrites/0/@type")
                .asText());
        assertEquals("availability(FREE)", node.at("/collection/rewrites/0/scope")
                .asText());
    }


    @Test
    public void testRemoveCorpusFromDifferentGroups () throws KustvaktException {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(corpusSigle=BRZ14 & textClass=wissenschaft) | (corpusSigle=AZPR | textClass=freizeit)");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals("koral:docGroup", node.at("/collection/operands/0/@type")
                .asText());
        assertEquals("koral:docGroup", node.at("/collection/operands/1/@type")
                .asText());
        assertEquals(1, node.at("/collection/operands/0/operands").size());
        assertEquals(1, node.at("/collection/operands/1/operands").size());
        assertEquals("koral:rewrite", node.at("/collection/rewrites/0/@type")
                .asText());
    }


    @Test
    public void testRemoveOneCorpusAndMoveDocFromTwoGroups ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(CollectionConstraint.class);
        // todo: use this collection query also to test clean up filter! after reduction of corpusSigle (wiss | freizeit)!
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(corpusSigle=BRZ14 & textClass=wissenschaft) | (corpusSigle=AZPR | textClass=freizeit)");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals("koral:doc", node.at("/collection/operands/0/@type")
                .asText());
        assertEquals("koral:doc", node.at("/collection/operands/0/@type")
                .asText());
        assertEquals("textClass", node.at("/collection/operands/0/key")
                .asText());
        assertEquals("wissenschaft", node.at("/collection/operands/0/value")
                .asText());
        assertEquals("koral:doc", node.at("/collection/operands/1/@type")
                .asText());
        assertEquals("textClass", node.at("/collection/operands/1/key")
                .asText());
        assertEquals("freizeit", node.at("/collection/operands/1/value")
                .asText());
        assertEquals("koral:rewrite", node.at("/collection/rewrites/0/@type")
                .asText());
    }



}
