package de.ids_mannheim.korap.web.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

/** Utility methods for maps
 * 
 * @author margaretha
 *
 */
public class MapUtils {

    /** Converts {@link MultivaluedMap} to {@link Map}
     * 
     * @param multivaluedMap
     * @return
     */
    public static Map<String, String> toMap (
            MultivaluedMap<String, String> multivaluedMap) {
        
        Set<String> keySet = multivaluedMap.keySet();
        Map<String, String> map = new HashMap<String, String>(keySet.size());
        
        for (String key :  keySet){
            List<String> values = multivaluedMap.get(key);
            String value = values.stream().collect(Collectors.joining(" "));
            map.put(key, value);
        }
        return map;
    }
}
