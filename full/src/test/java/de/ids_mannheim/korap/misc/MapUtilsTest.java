package de.ids_mannheim.korap.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import de.ids_mannheim.korap.web.utils.MapUtils;
import edu.emory.mathcs.backport.java.util.Arrays;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

public class MapUtilsTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertToMap() {
        MultivaluedMap<String, String> mm = new MultivaluedHashMap<String, String>();
        mm.put("k1", Arrays.asList(new String[]{"a", "b", "c"}));
        mm.put("k2", Arrays.asList(new String[]{"d", "e", "f"}));
        Map<String, String> map = MapUtils.toMap(mm);
        assertEquals(map.get("k1"), "a b c");
        assertEquals(map.get("k2"), "d e f");
    }

    @Test
    public void testConvertNullMap() {
        Map<String, String> map = MapUtils.toMap(null);
        assertEquals(0, map.size());
    }

    @Test
    public void testConvertEmptyMap() {
        MultivaluedMap<String, String> mm = new MultivaluedHashMap<String, String>();
        Map<String, String> map = MapUtils.toMap(mm);
        assertEquals(0, map.size());
    }
}
