package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.config.TestVariables;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl
 * @date 03/09/2015
 */
public class AvailabilityRewriteTest extends SpringJerseyTest {

    @Autowired
    public KustvaktConfiguration config;

    @Test
    public void testCollectionNodeRemoveCorpusIdNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
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
    public void testCollectionNodeDeletionNotification () {}

    @Test
    public void testCollectionNodeRemoveAllCorpusIdNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("corpusSigle=BRZ13 & corpusSigle=WPD");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(0, node.at("/collection/operands").size());
        assertEquals(node.at("/collection/rewrites/0/@type").asText(),
                "koral:rewrite");
    }

    @Test
    public void testCollectionNodeRemoveGroupedCorpusIdNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(corpusSigle=BRZ13 & textClass=Wissenschaft) & corpusSigle=WPD");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(node.at("/collection/operands/0/@type").asText(),
                "koral:docGroup");
        assertEquals(node.at("/collection/operands/0/operands/0/key").asText(),
                "textClass");
        assertEquals(node.at("/collection/rewrites/0/@type").asText(),
                "koral:rewrite");
    }

    // fixme: will probably fail when one doc groups are being refactored
    @Test
    public void testCollectionCleanEmptyDocGroupNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(corpusSigle=BRZ13 & corpusSigle=WPD) & textClass=Wissenschaft & textClass=Sport");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals(node.at("/collection/operands/0/key").asText(),
                "textClass");
        assertEquals(node.at("/collection/operands/1/key").asText(),
                "textClass");
        assertEquals(node.at("/collection/rewrites/0/@type").asText(),
                "koral:rewrite");
    }

    @Test
    public void testCollectionCleanMoveOneDocFromGroupUpNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(corpusSigle=BRZ13 & textClass=wissenschaft)");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/key").asText(), "textClass");
        assertEquals(node.at("/collection/value").asText(), "wissenschaft");
        assertEquals(node.at("/collection/rewrites/0/@type").asText(),
                "koral:rewrite");
    }

    @Test
    public void testCollectionCleanEmptyGroupAndMoveOneFromGroupUpNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(corpusSigle=BRZ13 & corpusSigle=WPD) & textClass=Wissenschaft");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/key").asText(), "textClass");
        assertEquals(node.at("/collection/rewrites/0/@type").asText(),
                "koral:rewrite");
    }

    @Test
    public void testCollectionRemoveAndMoveOneFromGroupUpNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(docID=random & textClass=Wissenschaft) & corpusSigle=WPD");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals(node.at("/collection/operands/0/@type").asText(),
                "koral:doc");
        assertEquals(node.at("/collection/operands/1/@type").asText(),
                "koral:doc");
        assertEquals(node.at("/collection/rewrites/0/@type").asText(),
                "koral:rewrite");
    }

    @Test
    public void testPublicCollectionRewriteEmptyAdd ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(AvailabilityRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(node.at("/collection/key").asText(), "availability");
        assertEquals(node.at("/collection/value").asText(), "CC.*");
        assertEquals(node.at("/collection/rewrites/0/@type").asText(),
                "koral:rewrite");
        assertEquals(freeCorpusAccess,
        		node.at("/collection/rewrites/0/_comment").asText());
    }

    @Test
    public void testPublicCollectionRewriteNonEmptyAdd ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(AvailabilityRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(docSigle=WPD_AAA & textClass=wissenschaft)");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals(node.at("/collection/operands/0/key").asText(),
                "availability");
        assertEquals(node.at("/collection/operands/0/value").asText(),
                "CC.*");
        assertEquals(node.at("/collection/operands/1/operands/0/key").asText(),
                "docSigle");
        assertEquals(node.at("/collection/operands/1/operands/1/key").asText(),
                "textClass");
        assertEquals(node.at("/collection/rewrites/0/@type").asText(),
                "koral:rewrite");
//        assertEquals(node.at("/collection/rewrites/0/scope").asText(),
//                "availability(FREE)");
    }

    @Test
    public void testRemoveCorpusFromDifferentGroups ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(corpusSigle=BRZ14 & textClass=wissenschaft) | (corpusSigle=AZPR | textClass=freizeit)");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals(node.at("/collection/operands/0/@type").asText(),
                "koral:docGroup");
        assertEquals(node.at("/collection/operands/1/@type").asText(),
                "koral:docGroup");
        assertEquals(1, node.at("/collection/operands/0/operands").size());
        assertEquals(1, node.at("/collection/operands/1/operands").size());
        assertEquals(node.at("/collection/rewrites/0/@type").asText(),
                "koral:rewrite");
    }

    @Test
    public void testRemoveOneCorpusAndMoveDocFromTwoGroups ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        // todo: use this collection query also to test clean up filter! after reduction of corpusSigle (wiss | freizeit)!
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(corpusSigle=BRZ14 & textClass=wissenschaft) | (corpusSigle=AZPR | textClass=freizeit)");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user")));
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals(node.at("/collection/operands/0/@type").asText(),
                "koral:doc");
        assertEquals(node.at("/collection/operands/0/@type").asText(),
                "koral:doc");
        assertEquals(node.at("/collection/operands/0/key").asText(),
                "textClass");
        assertEquals(node.at("/collection/operands/0/value").asText(),
                "wissenschaft");
        assertEquals(node.at("/collection/operands/1/@type").asText(),
                "koral:doc");
        assertEquals(node.at("/collection/operands/1/key").asText(),
                "textClass");
        assertEquals(node.at("/collection/operands/1/value").asText(),
                "freizeit");
        assertEquals(node.at("/collection/rewrites/0/@type").asText(),
                "koral:rewrite");
    }
}
