package de.ids_mannheim.korap.web.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author hanl
 * @date 21/01/2016
 */
public class KustvaktMap {

    private boolean mono;
    private Map<String, Object> values;

    public KustvaktMap() {
        this.values = new HashMap<>();
        this.mono = false;
    }

    public KustvaktMap(Map<String, Object> m) {
        this();
        setMap(m);
    }

    public void setMap(Map<String, Object> m) {
        if (!isGeneric(m) | !this.mono)
            this.values.putAll(m);
    }

    public boolean isGeneric() {
        return !this.mono && isGeneric(this.values);
    }

    private static boolean isGeneric(Map<String, Object> map) {
        int i = 0;
        for (Object o : map.values()) {
            if (o instanceof String)
                i++;
        }
        return !(i == map.size());
    }

    public void setMonoValue(boolean monovalue) {
        this.mono = monovalue;
    }

    public String get(String key) {
        Object o = this.values.get(key);
        if (!isGeneric())
            return (String) o;
        return String.valueOf(o);
    }

    public Object getRaw(String key) {
        return this.values.get(key);
    }

    public <T extends Object> Object get(String key, Class<T> cl) {
        if (isGeneric())
            return (T) this.values.get(key);
        return get(key);
    }

    public Set<String> keySet() {
        return this.values.keySet();
    }

}
