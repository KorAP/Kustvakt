package de.ids_mannheim.korap.misc;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.CollectionQueryProcessor;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.web.SearchKrill;

/**
 * @author hanl
 * @date 14/01/2016
 */
public class LocalQueryTest extends BeanConfigTest {

    private static String index;
    private static String qstring;


    @BeforeClass
    public static void setup () throws Exception {
        qstring = "creationDate since 1786 & creationDate until 1788";
        //        qstring = "creationDate since 1765 & creationDate until 1768";
        //        qstring = "textType = Aphorismus";
        //        qstring = "title ~ \"Werther\"";
    }


    @AfterClass
    public static void drop () {}


    @Test
    public void testQuery () {
        SearchKrill krill = new SearchKrill(index);
        KoralCollectionQueryBuilder coll = new KoralCollectionQueryBuilder();
        coll.with(qstring);
        String stats = krill.getStatistics(coll.toJSON());
        assert stats != null && !stats.isEmpty() && !stats.equals("null");
    }


    @Test
    public void testCollQuery () throws IOException {
        String qstring = "creationDate since 1800 & creationDate until 1820";
        CollectionQueryProcessor processor = new CollectionQueryProcessor();
        processor.process(qstring);

        String s = JsonUtils.toJSON(processor.getRequestMap());
        KrillCollection c = new KrillCollection(s);
        c.setIndex(new SearchKrill(index).getIndex());
        long docs = c.numberOf("documents");
        assert docs > 0 && docs < 15;
    }


    @Test
    public void testCollQuery2 () throws IOException {
        String query = "{\"@context\":\"http://korap.ids-mannheim.de/ns/koral/0.3/context.jsonld\",\"errors\":[],\"warnings\":[],\"messages\":[],\"collection\":{\"@type\":\"koral:docGroup\",\"operation\":\"operation:and\",\"operands\":[{\"@type\":\"koral:doc\",\"key\":\"creationDate\",\"type\":\"type:date\",\"value\":\"1786\",\"match\":\"match:geq\"},{\"@type\":\"koral:doc\",\"key\":\"creationDate\",\"type\":\"type:date\",\"value\":\"1788\",\"match\":\"match:leq\"}]},\"query\":{},\"meta\":{}}";
        KrillCollection c = new KrillCollection(query);
        c.setIndex(new SearchKrill(index).getIndex());
        long sent = c.numberOf("base/sentences");
        long docs = c.numberOf("documents");
    }


    @Test
    public void testQueryHash () {}


    @Override
    public void initMethod () throws KustvaktException {
//        helper().runBootInterfaces();
        index = helper().getContext().getConfiguration().getIndexDir();
    }
}
