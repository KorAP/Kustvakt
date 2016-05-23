import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.CollectionQueryBuilder3;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author hanl
 * @date 12/08/2015
 */
public class CollectionQueryBuilderTest {

    @Test
    public void testsimpleAdd () {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addQuery("corpusID=WPD");

        JsonNode node = JsonUtils.readTree(b.toJSON());

        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("corpusID", node.at("/collection/key").asText());
    }


    @Test
    public void testSimpleConjunction () {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addQuery("corpusID=WPD & textClass=freizeit");
        JsonNode node = JsonUtils.readTree(b.toJSON());

        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and", node.at("/collection/operation").asText());

        assertEquals("corpusID", node.at("/collection/operands/0/key").asText());
        assertEquals("textClass", node.at("/collection/operands/1/key")
                .asText());
    }


    @Test
    public void testSimpleDisjunction () {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addQuery("corpusID=WPD | textClass=freizeit");
        JsonNode node = JsonUtils.readTree(b.toJSON());

        System.out.println("_____________________________________________");
        System.out.println(node);

        assert node != null;
        assert node.at("/collection/operation").asText().equals("operation:or");
        assert node.at("/collection/operands/0/key").asText()
                .equals("corpusID");
        assert node.at("/collection/operands/1/key").asText()
                .equals("textClass");
    }


    @Test
    public void testComplexSubQuery () {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addQuery("(corpusID=WPD) | (textClass=freizeit & corpusID=WPD)");
        JsonNode node = JsonUtils.readTree(b.toJSON());

        System.out
                .println("_____________________________________________ COMPLEX");
        System.out.println(node);
        assert node != null;
        assert node.at("/collection/operation").asText().equals("operation:or");
        assert node.at("/collection/operands/0/key").asText()
                .equals("corpusID");
        assert node.at("/collection/operands/1/@type").asText()
                .equals("koral:docGroup");

    }


    @Test
    public void testAddResourceQueryAfter () {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addQuery("(corpusID=ADF) | (textClass=freizeit & corpusID=WPD)");

        CollectionQueryBuilder3 c = new CollectionQueryBuilder3();
        c.setBaseQuery(b.toJSON());
        c.addQuery("textClass=wissenschaft");

        JsonNode node = JsonUtils.readTree(c.toJSON());

        assert node != null;
        assert node.at("/collection/operands/2/@type").asText()
                .equals("koral:doc");
        assert node.at("/collection/operands/2/value").asText()
                .equals("wissenschaft");
    }


    @Test
    public void testAddComplexResourceQueryAfter () {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addQuery("(corpusID=ADF) | (textClass=freizeit & corpusID=WPD)");

        CollectionQueryBuilder3 c = new CollectionQueryBuilder3();
        c.setBaseQuery(b.toJSON());
        c.addQuery("(textClass=politik & corpusID=AZPS)");

        JsonNode node = JsonUtils.readTree(c.toJSON());

        assert node != null;
        assert node.at("/collection/operands/2/@type").asText()
                .equals("koral:docGroup");
        assert node.at("/collection/operands/2/operands/0/value").asText()
                .equals("politik");
        assert node.at("/collection/operands/2/operands/1/value").asText()
                .equals("AZPS");

    }


    @Test
    public void buildQuery () {
        String query = "[base=Haus]";
        QuerySerializer s = new QuerySerializer();
        s.setQuery(query, "poliqarp");
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addQuery("corpusID=WPD");
        s.setCollection("corpusID=WPD");

        System.out.println("QUERY " + s.toJSON());
    }


    @Test
    public void testBaseQueryBuild () {

    }

}
