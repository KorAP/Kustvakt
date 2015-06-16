package de.ids_mannheim.korap.utils;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author hanl
 * @date 01/07/2014
 */
public class PrefixTreeMap<V> extends TreeMap<String, V> {


    public SortedMap<String, V> getPrefixSubMap(String prefix) {
        if (prefix.length() > 0) {
            SortedMap d = this.subMap(prefix, getEnd(prefix));
            if (d.isEmpty())
                return null;
            return d;
        }
        return null;
    }

    private String getEnd(String prefix) {
        char nextLetter = (char) (prefix.charAt(prefix.length() - 1) + 1);
        return prefix.substring(0, prefix.length() - 1) + nextLetter;

    }

    public V getFirstValue(String prefix) {
        if (prefix.length() > 0) {
            String first = this.subMap(prefix, getEnd(prefix)).firstKey();
            return this.get(first);
        }
        return null;
    }

    public V getLastValue(String prefix) {
        if (prefix.length() > 0) {
            String last = this.subMap(prefix, getEnd(prefix)).lastKey();
            return this.get(last);
        }
        return null;
    }

}
