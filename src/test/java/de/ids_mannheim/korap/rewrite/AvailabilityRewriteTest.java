package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Disabled;
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
    
    private double apiVersion = Double.parseDouble(API_VERSION.substring(1));
    private String collectionNodeName = (apiVersion >= 1.1) ? "corpus"
			: "collection";

    @Disabled
    @Test
    public void testCollectionNodeRemoveCorpusIdNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("textClass=politik & corpusSigle=WPD");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user"), API_VERSION_DOUBLE));
        assertNotNull(node);
        assertEquals(1, node.at("/"+collectionNodeName+"/operands").size());
    }

    @Test
    public void testCollectionNodeDeletionNotification () {}

    @Disabled
    @Test
    public void testCollectionNodeRemoveAllCorpusIdNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("corpusSigle=BRZ13 & corpusSigle=WPD");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user"), API_VERSION_DOUBLE));
        assertNotNull(node);
        assertEquals(0, node.at("/"+collectionNodeName+"/operands").size());
        assertEquals("koral:rewrite", 
        		node.at("/"+collectionNodeName+"/rewrites/0/@type").asText());
    }
    
    @Disabled
    @Test
    public void testCollectionNodeRemoveGroupedCorpusIdNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(corpusSigle=BRZ13 & textClass=Wissenschaft) & corpusSigle=WPD");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user"), API_VERSION_DOUBLE));
        assertNotNull(node);
        assertEquals("koral:docGroup",
        		node.at("/"+collectionNodeName+"/operands/0/@type").asText());
        assertEquals("textClass",
        		node.at("/"+collectionNodeName+"/operands/0/operands/0/key").asText());
        assertEquals("koral:rewrite",
        		node.at("/"+collectionNodeName+"/rewrites/0/@type").asText());
    }

    // fixme: will probably fail when one doc groups are being refactored
    @Disabled
    @Test
    public void testCollectionCleanEmptyDocGroupNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(corpusSigle=BRZ13 & corpusSigle=WPD) & textClass=Wissenschaft & textClass=Sport");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user"), API_VERSION_DOUBLE));
        assertNotNull(node);
        assertEquals("koral:docGroup", 
        		node.at("/"+collectionNodeName+"/@type").asText());
        assertEquals(2, node.at("/"+collectionNodeName+"/operands").size());
        assertEquals("textClass", 
        		node.at("/"+collectionNodeName+"/operands/0/key").asText());
        assertEquals("textClass", 
        		node.at("/"+collectionNodeName+"/operands/1/key").asText());
        assertEquals("koral:rewrite",
        		node.at("/"+collectionNodeName+"/rewrites/0/@type").asText());
    }

    @Disabled
    @Test
    public void testCollectionCleanMoveOneDocFromGroupUpNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(corpusSigle=BRZ13 & textClass=wissenschaft)");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user"), API_VERSION_DOUBLE));
        assertNotNull(node);
        assertEquals("koral:doc", 
        		node.at("/"+collectionNodeName+"/@type").asText());
        assertEquals("textClass", 
        		node.at("/"+collectionNodeName+"/key").asText());
        assertEquals("wissenschaft", 
        		node.at("/"+collectionNodeName+"/value").asText());
        assertEquals("koral:rewrite", 
        		node.at("/"+collectionNodeName+"/rewrites/0/@type").asText());
    }

    @Disabled
    @Test
    public void testCollectionCleanEmptyGroupAndMoveOneFromGroupUpNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(corpusSigle=BRZ13 & corpusSigle=WPD) & textClass=Wissenschaft");
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user"), API_VERSION_DOUBLE));
        assertNotNull(node);
        assertEquals("koral:doc", 
        		node.at("/"+collectionNodeName+"/@type").asText());
        assertEquals("textClass", 
        		node.at("/"+collectionNodeName+"/key").asText());
        assertEquals("koral:rewrite", 
        		node.at("/"+collectionNodeName+"/rewrites/0/@type").asText());
    }

    @Disabled
    @Test
    public void testCollectionRemoveAndMoveOneFromGroupUpNoErrors ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(docID=random & textClass=Wissenschaft) & corpusSigle=WPD");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user"), API_VERSION_DOUBLE));
        assertNotNull(node);
        assertEquals("koral:docGroup", 
        		node.at("/"+collectionNodeName+"/@type").asText());
        assertEquals(2, node.at("/"+collectionNodeName+"/operands").size());
        assertEquals("koral:doc", 
        		node.at("/"+collectionNodeName+"/operands/0/@type").asText());
        assertEquals("koral:doc", 
        		node.at("/"+collectionNodeName+"/operands/1/@type").asText());
        assertEquals("koral:rewrite", 
        		node.at("/"+collectionNodeName+"/rewrites/0/@type").asText());
    }


    public void testPublicCollectionRewriteNonEmptyAdd ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(AvailabilityRewrite.class);
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection("(docSigle=WPD_AAA & textClass=wissenschaft)");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user"), API_VERSION_DOUBLE));
        assertNotNull(node);
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals("availability",
            node.at("/"+collectionNodeName+"/operands/0/key").asText());
        assertEquals("CC.*",
            node.at("/"+collectionNodeName+"/operands/0/value").asText());
        assertEquals("docSigle",
            node.at("/"+collectionNodeName+"/operands/1/operands/0/key").asText());
        assertEquals("textClass",
            node.at("/"+collectionNodeName+"/operands/1/operands/1/key").asText());
        assertEquals("koral:rewrite",
            node.at("/"+collectionNodeName+"/rewrites/0/@type").asText());
//        assertEquals(node.at("/collection/rewrites/0/scope").asText(),
//                "availability(FREE)");
    }
    
    @Test
    public void testPublicCollectionRewriteEmptyAdd ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(AvailabilityRewrite.class);
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user"), API_VERSION_DOUBLE));
        assertNotNull(node);
        assertEquals(node.at("/"+collectionNodeName+"/key").asText(), "availability");
        assertEquals(node.at("/"+collectionNodeName+"/value").asText(), "CC.*");
        assertEquals(node.at("/"+collectionNodeName+"/rewrites/0/@type").asText(),
                "koral:rewrite");
        assertEquals(freeCorpusAccess,
        		node.at("/"+collectionNodeName+"/rewrites/0/_comment").asText());
    }


    @Disabled
    @Test
    public void testRemoveCorpusFromDifferentGroups ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(corpusSigle=BRZ14 & textClass=wissenschaft) | (corpusSigle=AZPR | textClass=freizeit)");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user"), API_VERSION_DOUBLE));
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/"+collectionNodeName+"/@type").asText());
        assertEquals(2, node.at("/"+collectionNodeName+"/operands").size());
        assertEquals("koral:docGroup",
            node.at("/"+collectionNodeName+"/operands/0/@type").asText());
        assertEquals("koral:docGroup",
            node.at("/"+collectionNodeName+"/operands/1/@type").asText());
        assertEquals(1, node.at("/"+collectionNodeName+"/operands/0/operands").size());
        assertEquals(1, node.at("/"+collectionNodeName+"/operands/1/operands").size());
        assertEquals("koral:rewrite",
            node.at("/"+collectionNodeName+"/rewrites/0/@type").asText());
    }

    @Disabled
    @Test
    public void testRemoveOneCorpusAndMoveDocFromTwoGroups ()
            throws KustvaktException {
        RewriteHandler handler = new RewriteHandler(config);
        handler.add(CollectionConstraint.class);
        // todo: use this collection query also to test clean up filter! after reduction of corpusSigle (wiss | freizeit)!
        handler.add(CollectionCleanRewrite.class);
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setCollection(
                "(corpusSigle=BRZ14 & textClass=wissenschaft) | (corpusSigle=AZPR | textClass=freizeit)");
        String org = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(org,
                User.UserFactory.getUser("test_user"), API_VERSION_DOUBLE));
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/"+collectionNodeName+"/@type").asText());
        assertEquals(2, node.at("/"+collectionNodeName+"/operands").size());
        assertEquals("koral:doc",
            node.at("/"+collectionNodeName+"/operands/0/@type").asText());
        assertEquals("koral:doc",
            node.at("/"+collectionNodeName+"/operands/0/@type").asText());
        assertEquals("textClass",
            node.at("/"+collectionNodeName+"/operands/0/key").asText());
        assertEquals("wissenschaft",
            node.at("/"+collectionNodeName+"/operands/0/value").asText());
        assertEquals("koral:doc",
            node.at("/"+collectionNodeName+"/operands/1/@type").asText());
        assertEquals("textClass",
            node.at("/"+collectionNodeName+"/operands/1/key").asText());
        assertEquals("freizeit",
            node.at("/"+collectionNodeName+"/operands/1/value").asText());
        assertEquals("koral:rewrite",
            node.at("/"+collectionNodeName+"/rewrites/0/@type").asText());
    }
}
