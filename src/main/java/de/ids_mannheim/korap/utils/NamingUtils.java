package de.ids_mannheim.korap.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by hanl on 14.05.16.
 */
public class NamingUtils {

    private static final String SLASH = "/";


    private NamingUtils () {}


    public static Collection<String> joinStringSet (Collection<String> source,
            String other) {
        Set<String> set = new HashSet<>(source);
        set.add(other);
        return set;
    }


    public static Collection<UUID> joinUUIDSet (Collection<UUID> source,
            UUID other) {
        Set<UUID> set = new HashSet<>(source);
        set.add(other);
        return set;
    }


    public static String joinResources (String first, String second) {
        String res;
        if (first != null && !first.isEmpty())
            res = first + SLASH + second;
        else
            res = second;
        return res.replaceAll("\\s", "");
    }


    public static String[] splitAnnotations (String joined) {
        String[] spl = joined.split(SLASH);
        if (spl.length == 2)
            return spl;
        else
            return null;
    }


    public static String stripTokenType (String token) {
        int idx = token.lastIndexOf(" ");
        if (idx == -1)
            return token;
        return token.substring(idx).replaceAll("\\s", "");
    }


    public static String getTokenType (String token) {
        if (token.contains(" "))
            return token.substring(0, token.lastIndexOf(" "))
                    .replaceAll("\\s", "").toLowerCase();
        else
            return null;
    }


}
