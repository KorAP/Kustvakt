package de.ids_mannheim.korap.web.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hanl
 * @date 20/01/2016
 */
public class KustvaktResponseBuilder {
    Map<String, Object> _values;


    public KustvaktResponseBuilder () {
        this._values = new HashMap<>();
    }


    public KustvaktResponseBuilder addEntity (Object o) {
        if (o instanceof Map && !((Map) o).isEmpty())
            this._values.putAll((Map<? extends String, ?>) o);


        return this;
    }


    @Override
    public String toString () {
        return "";
    }

}
