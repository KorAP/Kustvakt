import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.utils.CollectionQueryBuilder3;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.Test;

/**
 * @author hanl
 * @date 12/08/2015
 */
public class CollectionQueryBuilderTest {

    @Test
    public void testsimpleAdd() {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "WPD");

        JsonNode node = JsonUtils.readTree(b.toJSON());

        assert node != null;
        assert node.at("/collection/@type").asText().equals("koral:doc");
        assert node.at("/collection/key").asText().equals("corpusID");

    }

    @Test
    public void testSimpleConjunction() {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "WPD").and()
                .addSegment("textClass", CollectionQueryBuilder3.EQ.EQUAL,
                        "freizeit");
        JsonNode node = JsonUtils.readTree(b.toJSON());

        assert node != null;
        assert node.at("/collection/@type").asText().equals("koral:docGroup");
        assert node.at("/collection/operation").asText()
                .equals("operation:and");
        assert node.at("/collection/operands/0/key").asText()
                .equals("corpusID");
        assert node.at("/collection/operands/1/key").asText()
                .equals("textClass");
    }

    @Test
    public void testSimpleDisjunction() {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "WPD").or()
                .addSegment("textClass", CollectionQueryBuilder3.EQ.EQUAL,
                        "freizeit");
        JsonNode node = JsonUtils.readTree(b.toJSON());

        assert node != null;
        assert node.at("/collection/operation").asText().equals("operation:or");
        assert node.at("/collection/operands/0/key").asText()
                .equals("corpusID");
        assert node.at("/collection/operands/1/key").asText()
                .equals("textClass");
    }

    @Test
    public void testComplexSubQuery() {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "ADF").or()
                .addSub("textClass=freizeit & corpusID=WPD");

        JsonNode node = JsonUtils.readTree(b.toJSON());

        assert node != null;
        assert node.at("/collection/operation").asText().equals("operation:or");
        assert node.at("/collection/operands/0/key").asText()
                .equals("corpusID");
        assert node.at("/collection/operands/1/@type").asText()
                .equals("koral:docGroup");

    }

    @Test
    public void testAddResourceQueryAfter() {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "ADF").or()
                .addSub("textClass=freizeit & corpusID=WPD");

        CollectionQueryBuilder3 c = new CollectionQueryBuilder3();
        c.setBaseQuery(b.toJSON());
        c.addSegment("textClass", CollectionQueryBuilder3.EQ.EQUAL,
                "wissenschaft");

        JsonNode node = JsonUtils.readTree(c.toJSON());

        assert node != null;
        assert node.at("/collection/operands/2/@type").asText()
                .equals("koral:doc");
        assert node.at("/collection/operands/2/value").asText()
                .equals("wissenschaft");
    }

    @Test
    public void testAddComplexResourceQueryAfter() {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "ADF").or()
                .addSub("textClass=freizeit & corpusID=WPD");

        CollectionQueryBuilder3 c = new CollectionQueryBuilder3();
        c.setBaseQuery(b.toJSON());
        c.addSub("(textClass=politik & corpusID=AZPS)");

        JsonNode node = JsonUtils.readTree(c.toJSON());

        assert node != null;
        assert node.at("/collection/operands/2/@type").asText()
                .equals("koral:docGroup");
        assert node.at("/collection/operands/2/operands/0/value").asText()
                .equals("politik");
        assert node.at("/collection/operands/2/operands/1/value").asText()
                .equals("AZPS");

    }

}
