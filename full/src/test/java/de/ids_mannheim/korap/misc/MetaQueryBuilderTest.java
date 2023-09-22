package de.ids_mannheim.korap.misc;

import de.ids_mannheim.korap.config.QueryBuilderUtil;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Created by hanl on 17.04.16.
 */
public class MetaQueryBuilderTest {

    @Test
    public void testSpanContext() {
        MetaQueryBuilder m = QueryBuilderUtil.defaultMetaBuilder(0, 1, 5, "sentence", false);
        Map<?, ?> map = m.raw();
        assertEquals(map.get("context"), "sentence");
        assertEquals(1, map.get("startPage"));
        assertEquals(0, map.get("startIndex"));
        assertEquals(false, map.get("cutOff"));
    }
}
