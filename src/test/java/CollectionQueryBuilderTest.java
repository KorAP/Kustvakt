import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author hanl
 * @date 12/08/2015
 */
public class CollectionQueryBuilderTest {

    @Test
    public void testsimpleAdd () {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("corpusSigle=WPD");

        JsonNode node = JsonUtils.readTree(b.toJSON());

        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("corpusSigle", node.at("/collection/key").asText());
    }


    @Test
    public void testSimpleConjunction () {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("corpusSigle=WPD & textClass=freizeit");
        JsonNode node = JsonUtils.readTree(b.toJSON());

        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and", node.at("/collection/operation").asText());

        assertEquals("corpusSigle", node.at("/collection/operands/0/key").asText());
        assertEquals("textClass", node.at("/collection/operands/1/key")
                .asText());
    }


    @Test
    public void testSimpleDisjunction () {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("corpusSigle=WPD | textClass=freizeit");
        JsonNode node = JsonUtils.readTree(b.toJSON());

        assertNotNull(node);
        assert node.at("/collection/operation").asText().equals("operation:or");
        assert node.at("/collection/operands/0/key").asText()
                .equals("corpusSigle");
        assert node.at("/collection/operands/1/key").asText()
                .equals("textClass");
    }


    @Test
    public void testComplexSubQuery () {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("(corpusSigle=WPD) | (textClass=freizeit & corpusSigle=BRZ13)");
        JsonNode node = JsonUtils.readTree(b.toJSON());

        assertNotNull(node);
        assert node.at("/collection/operation").asText().equals("operation:or");
        assert node.at("/collection/operands/0/key").asText()
                .equals("corpusSigle");
        assert node.at("/collection/operands/1/@type").asText()
                .equals("koral:docGroup");

    }


    @Test
    public void testAddResourceQueryAfter () {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("(textClass=politik & title=\"random title\") | textClass=wissenschaft");

        KoralCollectionQueryBuilder c = new KoralCollectionQueryBuilder();
        c.setBaseQuery(b.toJSON());
        c.with("corpusSigle=WPD");

        JsonNode node = JsonUtils.readTree(c.toJSON());
        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/operands/1/@type")
                .asText());
        assertEquals("koral:docGroup", node.at("/collection/operands/0/@type")
                .asText());
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals(2, node.at("/collection/operands/0/operands").size());
        assertEquals(2, node.at("/collection/operands/0/operands/0/operands").size());

        assertEquals("operation:and", node.at("/collection/operation").asText());
        assertEquals("operation:or", node.at("/collection/operands/0/operation").asText());
        assertEquals("operation:and", node.at("/collection/operands/0/operands/0/operation").asText());
        assertEquals("WPD", node.at("/collection/operands/1/value").asText());
    }


    @Test
    public void testAddComplexResourceQueryAfter () {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("(title=\"random title\") | (textClass=wissenschaft)");

        KoralCollectionQueryBuilder c = new KoralCollectionQueryBuilder();
        c.setBaseQuery(b.toJSON());
        c.with("(corpusSigle=BRZ13 | corpusSigle=AZPS)");

        JsonNode node = JsonUtils.readTree(c.toJSON());
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/operands/0/@type")
                .asText());
        assertEquals("koral:docGroup", node.at("/collection/operands/1/@type")
                .asText());
        assertEquals("BRZ13", node
                .at("/collection/operands/1/operands/0/value").asText());
        assertEquals("AZPS", node.at("/collection/operands/1/operands/1/value")
                .asText());
        assertEquals("random title",
                node.at("/collection/operands/0/operands/0/value").asText());
        assertEquals("wissenschaft",
                node.at("/collection/operands/0/operands/1/value").asText());
    }


    @Test
    public void testBuildQuery () {
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
        assertEquals("koral:docGroup", res.at("/collection/@type").asText());
        assertEquals("operation:and", res.at("/collection/operation").asText());
        assertEquals("koral:doc", res.at("/collection/operands/0/@type")
                .asText());
        assertEquals("freizeit", res.at("/collection/operands/1/value")
                .asText());
        assertEquals("textClass", res.at("/collection/operands/1/key").asText());

        assertEquals("koral:doc", res.at("/collection/operands/1/@type")
                .asText());
        assertEquals("WPD", res.at("/collection/operands/0/value").asText());
        assertEquals("corpusSigle", res.at("/collection/operands/0/key").asText());

        // check also that query is still there
        assertEquals("koral:token", res.at("/query/@type").asText());
        assertEquals("koral:term", res.at("/query/wrap/@type").asText());
        assertEquals("Haus", res.at("/query/wrap/key").asText());
        assertEquals("lemma", res.at("/query/wrap/layer").asText());
    }


    @Test
    public void testBaseQueryBuild () {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("(corpusSigle=ADF) | (textClass=freizeit & corpusSigle=WPD)");

        KoralCollectionQueryBuilder c = new KoralCollectionQueryBuilder();
        c.setBaseQuery(b.toJSON());

        c.with("corpusSigle=BRZ13");
        JsonNode base = (JsonNode) c.rebaseCollection();
        assertNotNull(base);
        assertEquals(base.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(base.at("/collection/operands/1/@type").asText(),
                "koral:doc");
        assertEquals(base.at("/collection/operands/1/value").asText(), "BRZ13");
        assertEquals(base.at("/collection/operands/0/@type").asText(),
                "koral:docGroup");
        assertEquals(base.at("/collection/operands/0/operands").size(), 2);
    }

    @Test
    public void testNodeMergeWithBase() {
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
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and", node.at("/collection/operation").asText());
        assertEquals(2, node.at("/collection/operands").size());
    }


    @Test
    public void testStoredCollectionBaseQueryBuild () {

    }

    @Test
    public void testAddOROperator() {
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
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:or", node.at("/collection/operation").asText());
        assertEquals(2, node.at("/collection/operands/1/operands").size());
    }

    @Test
    public void testAddANDOperator() {
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
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and", node.at("/collection/operation").asText());
        assertEquals(2, node.at("/collection/operands/1/operands").size());
    }

    @Test
    public void testAddDefaultOperator() {
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
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and", node.at("/collection/operation").asText());
        assertEquals(2, node.at("/collection/operands/1/operands").size());
    }

    @Test
    @Ignore
    public void testMergeOperator() {
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
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and", node.at("/collection/operation").asText());
        assertEquals(2, node.at("/collection/operands").size());
    }

}
