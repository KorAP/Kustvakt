package de.ids_mannheim.korap.web.utils;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to wrapp multivaluedmap into a hashmap. Depending on the strict parameter,
 * list values are retained in the resulting wrapper map.
 *
 * @author hanl
 * @date 18/05/2015
 */
public class FormWrapper extends HashMap<String, Object> {

    public FormWrapper(MultivaluedMap form, boolean strict) {
        super(toMap(form, strict));
    }

    public FormWrapper(MultivaluedMap form) {
        super(toMap(form, true));
    }

    private static HashMap<String, Object> toMap(MultivaluedMap<String, Object> form,
            boolean strict) {
        HashMap<String, Object> map = new HashMap<>();
        for (Map.Entry<String, List<Object>> e : form.entrySet()) {
            if (e.getValue().size() == 1)
                map.put(e.getKey(), e.getValue().get(0));
            else if (!strict)
                map.put(e.getKey(), e.getValue());
        }
        return map;
    }
}
