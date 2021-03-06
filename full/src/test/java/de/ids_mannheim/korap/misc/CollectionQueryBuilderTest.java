package de.ids_mannheim.korap.misc;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;

/**
 * @author hanl
 * @date 12/08/2015
 */
public class CollectionQueryBuilderTest {

    @Test
    public void testsimpleAdd () throws KustvaktException {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("corpusSigle=WPD");

        JsonNode node = JsonUtils.readTree(b.toJSON());

        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("corpusSigle", node.at("/collection/key").asText());
    }


    @Test
    public void testSimpleConjunction () throws KustvaktException {
        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.with("corpusSigle=WPD & textClass=freizeit");
        JsonNode node = JsonUtils.readTree(b.toJSON());

        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and", node.at("/collection/operation").asText());

        assertEquals("corpusSigle", node.at("/collection/operands/0/key")
                .asText());
        assertEquals("textClass", node.at("/collection/operands/1/key")
                .asText());
    }


    @Test
    public void testSimpleDisjunction () throws KustvaktException {
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
    public void testComplexSubQuery () throws KustvaktException {
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
    public void testAddResourceQueryAfter () throws KustvaktException {
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
        assertEquals(2, node.at("/collection/operands/0/operands/0/operands")
                .size());

        assertEquals("operation:and", node.at("/collection/operation").asText());
        assertEquals("operation:or", node
                .at("/collection/operands/0/operation").asText());
        assertEquals("operation:and",
                node.at("/collection/operands/0/operands/0/operation").asText());
        assertEquals("WPD", node.at("/collection/operands/1/value").asText());
    }


    @Test
    public void testAddComplexResourceQueryAfter () throws KustvaktException {
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
    public void testBuildQuery () throws KustvaktException {
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
        assertEquals("corpusSigle", res.at("/collection/operands/0/key")
                .asText());

        // check also that query is still there
        assertEquals("koral:token", res.at("/query/@type").asText());
        assertEquals("koral:term", res.at("/query/wrap/@type").asText());
        assertEquals("Haus", res.at("/query/wrap/key").asText());
        assertEquals("lemma", res.at("/query/wrap/layer").asText());
    }


    @Test
    public void testBaseQueryBuild () throws KustvaktException {
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
    public void testNodeMergeWithBase () throws KustvaktException {
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
    public void testNodeMergeWithoutBase () throws KustvaktException {
        String query = "[base=Haus]";
        QuerySerializer check = new QuerySerializer();
        check.setQuery(query, "poliqarp");

        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.setBaseQuery(check.toJSON());

        KoralCollectionQueryBuilder test = new KoralCollectionQueryBuilder();
        test.with("corpusSigle=WPD");
//        String json = test.toJSON();
//        System.out.println(json);
        //JsonNode node = (JsonNode) test.rebaseCollection(null);
        //node = b.mergeWith(node);
        //assertNotNull(node);
        //assertEquals("koral:doc", node.at("/collection/@type").asText());
        //assertEquals("corpusSigle", node.at("/collection/key").asText());
    }


    @Test
    public void testNodeMergeWithoutBaseWrongOperator () throws KustvaktException {
        String query = "[base=Haus]";
        QuerySerializer check = new QuerySerializer();
        check.setQuery(query, "poliqarp");

        KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
        b.setBaseQuery(check.toJSON());

        KoralCollectionQueryBuilder test = new KoralCollectionQueryBuilder();
        // operator is not supposed to be here!
        test.and().with("corpusSigle=WPD");
//        String json = test.toJSON();
//        System.out.println(json);
        //JsonNode node = (JsonNode) test.rebaseCollection(null);
        //node = b.mergeWith(node);
        //assertNotNull(node);
        //assertEquals("koral:doc", node.at("/collection/@type").asText());
        //assertEquals("corpusSigle", node.at("/collection/key").asText());
    }


    @Test
    public void testStoredCollectionBaseQueryBuild () {

    }


    @Test
    public void testAddOROperator () throws KustvaktException {
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
    public void testAddANDOperator () throws KustvaktException {
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
    public void testAddDefaultOperator () throws KustvaktException {
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
    public void testBaseCollectionNull () throws KustvaktException {
        // base is missing collection segment
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");

        KoralCollectionQueryBuilder total = new KoralCollectionQueryBuilder();
        total.setBaseQuery(s.toJSON());

        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
        builder.with("textClass=politik & corpusSigle=WPD");
        JsonNode node = total.and().mergeWith(
                (JsonNode) builder.rebaseCollection());

        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and", node.at("/collection/operation").asText());
        assertEquals("koral:doc", node.at("/collection/operands/0/@type")
                .asText());
        assertEquals("koral:doc", node.at("/collection/operands/1/@type")
                .asText());
        assertEquals("textClass", node.at("/collection/operands/0/key")
                .asText());
        assertEquals("corpusSigle", node.at("/collection/operands/1/key")
                .asText());
    }


    @Test
    public void testMergeCollectionNull () throws KustvaktException {
        // merge json is missing collection segment
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        s.setCollection("textClass=wissenschaft");

        KoralCollectionQueryBuilder total = new KoralCollectionQueryBuilder();
        total.setBaseQuery(s.toJSON());

        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
        JsonNode node = total.and().mergeWith(
                (JsonNode) builder.rebaseCollection());
        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("textClass", node.at("/collection/key").asText());
    }


}
