package de.ids_mannheim.korap.web.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to wrapp multivaluedmap into a hashmap. Depending on the strict parameter,
 * list values are retained in the resulting wrapper map.
 *
 * @author hanl
 * @date 25/04/2015
 */
public class FormRequestWrapper extends HttpServletRequestWrapper {

    private MultivaluedMap<String, String> form;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request
     * @throws IllegalArgumentException if the request is null
     */
    public FormRequestWrapper(HttpServletRequest request,
            MultivaluedMap<String, String> form) {
        super(request);
        this.form = form;
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if (value == null)
            value = String.valueOf(form.getFirst(name));
        return value;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null && form.get(name) != null) {
            values = new String[form.get(name).size()];
            values = form.get(name).toArray(values);
        }
        return values;
    }

    public Map<String, String> singleValueMap() {
        return toMap(this.form, false);
    }

    /**
     * @param strict returns only values with size equal to one. If false pairs key to first value
     *               in value list and returns the result
     * @return key/value map
     */
    public static Map<String, String> toMap(MultivaluedMap<String, String> form,
            boolean strict) {
        HashMap<String, String> map = new HashMap<>();
        for (String key : form.keySet()) {
            if (strict && form.get(key).size() > 1)
                continue;
            map.put(key, form.getFirst(key));

        }
        return map;
    }

    public void put(String key, String value) {
        this.form.putSingle(key, value);
    }

    public void put(String key, String... values) {
        this.form.put(key, Arrays.<String>asList(values));
    }

}


