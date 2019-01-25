package de.ids_mannheim.korap.misc;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;

import de.ids_mannheim.korap.web.utils.MapUtils;
import edu.emory.mathcs.backport.java.util.Arrays;

public class MapUtilsTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertToMap () {
        MultivaluedMap<String, String> mm =
                new MultivaluedHashMap<String, String>();
        mm.put("k1", Arrays.asList(new String[] { "a", "b", "c" }));
        mm.put("k2", Arrays.asList(new String[] { "d", "e", "f" }));

        Map<String, String> map = MapUtils.toMap(mm);
        assertEquals("a b c", map.get("k1"));
        assertEquals("d e f", map.get("k2"));
    }

    @Test
    public void testConvertNullMap () {
        Map<String, String> map = MapUtils.toMap(null);
        assertEquals(0, map.size());
    }

    @Test
    public void testConvertEmptyMap () {
        MultivaluedMap<String, String> mm =
                new MultivaluedHashMap<String, String>();
        Map<String, String> map = MapUtils.toMap(mm);
        assertEquals(0, map.size());
    }

}
