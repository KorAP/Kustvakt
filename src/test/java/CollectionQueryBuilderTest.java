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
        //        System.out.println(b.toJSON());
    }

    @Test
    public void testSimpleConjunctive() {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "WPD").and()
                .addSegment("textClass", CollectionQueryBuilder3.EQ.EQUAL,
                        "freizeit");
        //        System.out.println(b.toJSON());
    }

    @Test
    public void testSimpleDisjunctive() {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "WPD").and()
                .addSegment("textClass", CollectionQueryBuilder3.EQ.EQUAL,
                        "freizeit");
        //        System.out.println(b.toJSON());
    }

    @Test
    public void testComplexSubQuery() {

        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "ADF").and()
                .addSub("textClass=freizeit & corpusID=WPD");
        //        System.out.println(b.toJSON());
    }

    @Test // basically joining two or more resource queries
    public void testAddResourceQueryAfter() {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "ADF").and()
                .addSub("textClass=freizeit & corpusID=WPD");
        //        System.out.println(b.toJSON());

        //        join.addSegment("textClass", "politik");
    }

    @Test // basically joining two or more resource queries
    public void testAddResourceQueryBefore() {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "ADF").and()
                .addSub("textClass!=freizeit & corpusID=WPD");

        //        CollectionQueryBuilder3 join = new CollectionQueryBuilder3();
        //        join.addRaw(b.toJSON());
        //        join.addSegment("textClass", "politik");
        //        System.out.println("JOINED " + join.toJSON());
    }

    @Test
    public void test1() {
        CollectionQueryBuilder3 b = new CollectionQueryBuilder3();
        b.addSegment("corpusID", CollectionQueryBuilder3.EQ.EQUAL, "ADF").or()
                .addSub("textClass=freizeit & corpusID=WPD");

        CollectionQueryBuilder3 c = new CollectionQueryBuilder3();
        c.setBaseQuery(b.toJSON());
        c.addSub("textClass=wissenschaft");

        JsonNode node = JsonUtils.readTree(c.toJSON());

        assert node != null;
        assert node.at("/collection/operands/2/@type").asText()
                .equals("koral:doc");
        assert node.at("/collection/operands/2/value").asText()
                .equals("wissenschaft");
    }

}
