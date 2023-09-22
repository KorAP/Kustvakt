package de.ids_mannheim.korap.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author hanl
 * @date 12/08/2015
 */
@DisplayName("Collection Query Builder Test")
class CollectionQueryBuilderTest {

    @Test
    @DisplayName("Testsimple Add")
    void testsimpleAdd() throws KustvaktException {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("corpusSigle=WPD");
        JsonNode node = JsonUtils.readTree(b.toJSON());
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/key").asText(), "corpusSigle");
    }

    @Test
    @DisplayName("Test Simple Conjunction")
    void testSimpleConjunction() throws KustvaktException {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("corpusSigle=WPD & textClass=freizeit");
        JsonNode node = JsonUtils.readTree(b.toJSON());
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operation").asText(), "operation:and");
        assertEquals(node.at("/collection/operands/0/key").asText(), "corpusSigle");
        assertEquals(node.at("/collection/operands/1/key").asText(), "textClass");
    }

    @Test
    @DisplayName("Test Simple Disjunction")
    void testSimpleDisjunction() throws KustvaktException {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("corpusSigle=WPD | textClass=freizeit");
        JsonNode node = JsonUtils.readTree(b.toJSON());
        assertNotNull(node);
        assert node.at("/collection/operation").asText().equals("operation:or");
        assert node.at("/collection/operands/0/key").asText().equals("corpusSigle");
        assert node.at("/collection/operands/1/key").asText().equals("textClass");
    }

    @Test
    @DisplayName("Test Complex Sub Query")
    void testComplexSubQuery() throws KustvaktException {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("(corpusSigle=WPD) | (textClass=freizeit & corpusSigle=BRZ13)");
        JsonNode node = JsonUtils.readTree(b.toJSON());
        assertNotNull(node);
        assert node.at("/collection/operation").asText().equals("operation:or");
        assert node.at("/collection/operands/0/key").asText().equals("corpusSigle");
        assert node.at("/collection/operands/1/@type").asText().equals("koral:docGroup");
    }

    @Test
    @DisplayName("Test Add Resource Query After")
    void testAddResourceQueryAfter() throws KustvaktException {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("(textClass=politik & title=\"random title\") | textClass=wissenschaft");
        KoralCollectionQueryBuilder c = new KoralCollectionQueryBuilder();
        c.setBaseQuery(b.toJSON());
        c.with("corpusSigle=WPD");
        JsonNode node = JsonUtils.readTree(c.toJSON());
        assertNotNull(node);
        assertEquals(node.at("/collection/operands/1/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/operands/0/@type").asText(), "koral:docGroup");
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals(2, node.at("/collection/operands/0/operands").size());
        assertEquals(2, node.at("/collection/operands/0/operands/0/operands").size());
        assertEquals(node.at("/collection/operation").asText(), "operation:and");
        assertEquals(node.at("/collection/operands/0/operation").asText(), "operation:or");
        assertEquals(node.at("/collection/operands/0/operands/0/operation").asText(), "operation:and");
        assertEquals(node.at("/collection/operands/1/value").asText(), "WPD");
    }

    @Test
    @DisplayName("Test Add Complex Resource Query After")
    void testAddComplexResourceQueryAfter() throws KustvaktException {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("(title=\"random title\") | (textClass=wissenschaft)");
        KoralCollectionQueryBuilder c = new KoralCollectionQueryBuilder();
        c.setBaseQuery(b.toJSON());
        c.with("(corpusSigle=BRZ13 | corpusSigle=AZPS)");
        JsonNode node = JsonUtils.readTree(c.toJSON());
        assertNotNull(node);
        assertEquals(node.at("/collection/operands/0/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operands/1/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operands/1/operands/0/value").asText(), "BRZ13");
        assertEquals(node.at("/collection/operands/1/operands/1/value").asText(), "AZPS");
        assertEquals(node.at("/collection/operands/0/operands/0/value").asText(), "random title");
        assertEquals(node.at("/collection/operands/0/operands/1/value").asText(), "wissenschaft");
    }

    @Test
    @DisplayName("Test Build Query")
    void testBuildQuery() throws KustvaktException {
        String coll = "corpusSigle=WPD";
        String query = "[base=Haus]";
        QuerySerializer check = new QuerySerializer();
        check.setQuery(query, "poliqarp");
        check.setCollection(coll);
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.setBaseQuery(check.toJSON());
        b.with("textClass=freizeit");
        JsonNode res = (JsonNode) b.rebaseCollection();
        assertNotNull(res);
        assertEquals(res.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(res.at("/collection/operation").asText(), "operation:and");
        assertEquals(res.at("/collection/operands/0/@type").asText(), "koral:doc");
        assertEquals(res.at("/collection/operands/1/value").asText(), "freizeit");
        assertEquals(res.at("/collection/operands/1/key").asText(), "textClass");
        assertEquals(res.at("/collection/operands/1/@type").asText(), "koral:doc");
        assertEquals(res.at("/collection/operands/0/value").asText(), "WPD");
        assertEquals(res.at("/collection/operands/0/key").asText(), "corpusSigle");
        // check also that query is still there
        assertEquals(res.at("/query/@type").asText(), "koral:token");
        assertEquals(res.at("/query/wrap/@type").asText(), "koral:term");
        assertEquals(res.at("/query/wrap/key").asText(), "Haus");
        assertEquals(res.at("/query/wrap/layer").asText(), "lemma");
    }

    @Test
    @DisplayName("Test Base Query Build")
    void testBaseQueryBuild() throws KustvaktException {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("(corpusSigle=ADF) | (textClass=freizeit & corpusSigle=WPD)");
        KoralCollectionQueryBuilder c = new KoralCollectionQueryBuilder();
        c.setBaseQuery(b.toJSON());
        c.with("corpusSigle=BRZ13");
        JsonNode base = (JsonNode) c.rebaseCollection();
        assertNotNull(base);
        assertEquals(base.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(base.at("/collection/operands/1/@type").asText(), "koral:doc");
        assertEquals(base.at("/collection/operands/1/value").asText(), "BRZ13");
        assertEquals(base.at("/collection/operands/0/@type").asText(), "koral:docGroup");
        assertEquals(base.at("/collection/operands/0/operands").size(), 2);
    }

    @Test
    @DisplayName("Test Node Merge With Base")
    void testNodeMergeWithBase() throws KustvaktException {
        String coll = "corpusSigle=WPD";
        String query = "[base=Haus]";
        QuerySerializer check = new QuerySerializer();
        check.setQuery(query, "poliqarp");
        check.setCollection(coll);
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.setBaseQuery(check.toJSON());
        KoralCollectionQueryBuilder test = new KoralCollectionQueryBuilder();
        test.with("textClass=wissenschaft | textClass=politik");
        JsonNode node = (JsonNode) test.rebaseCollection();
        node = b.mergeWith(node);
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operation").asText(), "operation:and");
        assertEquals(2, node.at("/collection/operands").size());
    }

    @Test
    @DisplayName("Test Node Merge Without Base")
    void testNodeMergeWithoutBase() throws KustvaktException {
        String query = "[base=Haus]";
        QuerySerializer check = new QuerySerializer();
        check.setQuery(query, "poliqarp");
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.setBaseQuery(check.toJSON());
        KoralCollectionQueryBuilder test = new KoralCollectionQueryBuilder();
        test.with("corpusSigle=WPD");
        // String json = test.toJSON();
        // System.out.println(json);
        // JsonNode node = (JsonNode) test.rebaseCollection(null);
        // node = b.mergeWith(node);
        // assertNotNull(node);
        // assertEquals("koral:doc", node.at("/collection/@type").asText());
        // assertEquals("corpusSigle", node.at("/collection/key").asText());
    }

    @Test
    @DisplayName("Test Node Merge Without Base Wrong Operator")
    void testNodeMergeWithoutBaseWrongOperator() throws KustvaktException {
        String query = "[base=Haus]";
        QuerySerializer check = new QuerySerializer();
        check.setQuery(query, "poliqarp");
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.setBaseQuery(check.toJSON());
        KoralCollectionQueryBuilder test = new KoralCollectionQueryBuilder();
        // operator is not supposed to be here!
        test.and().with("corpusSigle=WPD");
        // String json = test.toJSON();
        // System.out.println(json);
        // JsonNode node = (JsonNode) test.rebaseCollection(null);
        // node = b.mergeWith(node);
        // assertNotNull(node);
        // assertEquals("koral:doc", node.at("/collection/@type").asText());
        // assertEquals("corpusSigle", node.at("/collection/key").asText());
    }

    @Test
    @DisplayName("Test Stored Collection Base Query Build")
    void testStoredCollectionBaseQueryBuild() {
    }

    @Test
    @DisplayName("Test Add OR Operator")
    void testAddOROperator() throws KustvaktException {
        String coll = "corpusSigle=WPD";
        String query = "[base=Haus]";
        QuerySerializer check = new QuerySerializer();
        check.setQuery(query, "poliqarp");
        check.setCollection(coll);
        KoralCollectionQueryBuilder test = new KoralCollectionQueryBuilder();
        test.setBaseQuery(check.toJSON());
        test.or().with("textClass=wissenschaft | textClass=politik");
        JsonNode node = (JsonNode) test.rebaseCollection();
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operation").asText(), "operation:or");
        assertEquals(2, node.at("/collection/operands/1/operands").size());
    }

    @Test
    @DisplayName("Test Add AND Operator")
    void testAddANDOperator() throws KustvaktException {
        String coll = "corpusSigle=WPD";
        String query = "[base=Haus]";
        QuerySerializer check = new QuerySerializer();
        check.setQuery(query, "poliqarp");
        check.setCollection(coll);
        KoralCollectionQueryBuilder test = new KoralCollectionQueryBuilder();
        test.setBaseQuery(check.toJSON());
        test.and().with("textClass=wissenschaft | textClass=politik");
        JsonNode node = (JsonNode) test.rebaseCollection();
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operation").asText(), "operation:and");
        assertEquals(2, node.at("/collection/operands/1/operands").size());
    }

    @Test
    @DisplayName("Test Add Default Operator")
    void testAddDefaultOperator() throws KustvaktException {
        String coll = "corpusSigle=WPD";
        String query = "[base=Haus]";
        QuerySerializer check = new QuerySerializer();
        check.setQuery(query, "poliqarp");
        check.setCollection(coll);
        KoralCollectionQueryBuilder test = new KoralCollectionQueryBuilder();
        test.setBaseQuery(check.toJSON());
        test.with("textClass=wissenschaft | textClass=politik");
        JsonNode node = (JsonNode) test.rebaseCollection();
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operation").asText(), "operation:and");
        assertEquals(2, node.at("/collection/operands/1/operands").size());
    }

    @Test
    @DisplayName("Test Base Collection Null")
    void testBaseCollectionNull() throws KustvaktException {
        // base is missing collection segment
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        KoralCollectionQueryBuilder total = new KoralCollectionQueryBuilder();
        total.setBaseQuery(s.toJSON());
        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
        builder.with("textClass=politik & corpusSigle=WPD");
        JsonNode node = total.and().mergeWith((JsonNode) builder.rebaseCollection());
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operation").asText(), "operation:and");
        assertEquals(node.at("/collection/operands/0/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/operands/1/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/operands/0/key").asText(), "textClass");
        assertEquals(node.at("/collection/operands/1/key").asText(), "corpusSigle");
    }

    @Test
    @DisplayName("Test Merge Collection Null")
    void testMergeCollectionNull() throws KustvaktException {
        // merge json is missing collection segment
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        s.setCollection("textClass=wissenschaft");
        KoralCollectionQueryBuilder total = new KoralCollectionQueryBuilder();
        total.setBaseQuery(s.toJSON());
        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
        JsonNode node = total.and().mergeWith((JsonNode) builder.rebaseCollection());
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/key").asText(), "textClass");
    }
}
