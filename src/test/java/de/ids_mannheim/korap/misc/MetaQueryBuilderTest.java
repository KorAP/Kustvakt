import de.ids_mannheim.korap.config.QueryBuilderUtil;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by hanl on 17.04.16.
 */
public class MetaQueryBuilderTest {



    @Test
    public void testSpanContext () {
        System.out.println("____________________-");
        MetaQueryBuilder m = QueryBuilderUtil.defaultMetaBuilder(0, 1, 5,
                "sentence", false);
        Map map = m.raw();

        assertEquals("sentence", map.get("context"));
        assertEquals(1, map.get("startPage"));
        assertEquals(0, map.get("startIndex"));
        assertEquals(false, map.get("cufOff"));

    }
}
