package de.ids_mannheim.korap.utils;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;

import java.util.*;

public class StringUtils {
    private final static Logger jlog = KorAPLogger.initiate(StringUtils.class);

    private static final String SLASH = "/";
    private static final String SEP = ";";

    public static Collection<UUID> stringToUUIDList(String s) {
        String[] array = s.split(SEP);
        List<UUID> list = new LinkedList<>();
        for (String att : array) {
            list.add(UUID.fromString(att));
        }
        return list;
    }

    public static List<String> toList(String values) {
        List<String> list = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(values, SEP);
        while (tokenizer.hasMoreTokens())
            list.add(tokenizer.nextToken());
        return list;
    }

    public static Set<String> toSet(String values, String sep) {
        Set<String> set = new HashSet<>();
        StringTokenizer tokenizer = new StringTokenizer(values, sep);
        while (tokenizer.hasMoreTokens())
            set.add(tokenizer.nextToken());
        return set;
    }

    public static Set<String> toSet(String values) {
        return toSet(values, SEP);
    }

    public static String toString(Collection<String> values) {
        return StringUtils.toString(values, SEP);
    }

    public static String toString(Collection<String> values, String sep) {
        StringBuffer b = new StringBuffer();
        for (String s : values)
            b.append(s).append(sep);

        if (b.length() > 0)
            b.deleteCharAt(b.length() - 1);
        return b.toString();
    }

    public static String orderedToString(Collection<String> hash) {
        Set<String> orderedSet = new TreeSet<>();
        orderedSet.addAll(hash);
        if (orderedSet.isEmpty()) {
            return "";
        }else {
            StringBuilder builder = new StringBuilder();
            for (String s : orderedSet) {
                builder.append(s);
                builder.append(SEP);
            }
            builder.deleteCharAt(builder.length() - 1);
            return builder.toString();
        }
    }

    public static String UUIDsetToString(Collection<UUID> hash) {
        Set<UUID> orderedSet = new TreeSet<>();
        orderedSet.addAll(hash);
        if (orderedSet.isEmpty()) {
            return "";
        }else {
            StringBuilder builder = new StringBuilder();
            for (UUID s : orderedSet) {
                builder.append(s);
                builder.append(SEP);
            }
            builder.deleteCharAt(builder.length() - 1);
            return builder.toString();
        }
    }

    public static String buildSQLRegex(String path) {
        StringBuilder b = new StringBuilder();
        String[] match = path.split("/");
        b.append(match[0]);
        b.append("(" + "/" + match[1] + ")");
        b.append("*$");
        return b.toString();
    }

    public static Collection<String> joinStringSet(Collection<String> source,
            String other) {
        Set<String> set = new HashSet<>(source);
        set.add(other);
        return set;
    }

    public static Collection<UUID> joinUUIDSet(Collection<UUID> source,
            UUID other) {
        Set<UUID> set = new HashSet<>(source);
        set.add(other);
        return set;
    }

    public static String joinResources(String first, String second) {
        String res;
        if (first != null && !first.isEmpty())
            res = first + SLASH + second;
        else
            res = second;
        return res.replaceAll("\\s", "");
    }

    public static String[] splitAnnotations(String joined) {
        String[] spl = joined.split(SLASH);
        if (spl.length == 2)
            return spl;
        else
            return null;
    }

    public static String stripTokenType(String token) {
        int idx = token.lastIndexOf(" ");
        if (idx == -1)
            return token;
        return token.substring(idx).replaceAll("\\s", "");
    }

    public static String getTokenType(String token) {
        return token.substring(0, token.lastIndexOf(" ")).replaceAll("\\s", "")
                .toLowerCase();
    }

    public static boolean isInteger(String value) {
        try {
            Integer.valueOf(value);
            return true;
        }catch (IllegalArgumentException e) {
            // do nothing!
            return false;
        }
    }

    public static String normalize(String value) {
        return value.trim().toLowerCase();
    }

    public static String normalizeHTML(String value) {
        return StringEscapeUtils.escapeHtml4(value);
    }

    public static String decodeHTML(String value) {
        return StringEscapeUtils.unescapeHtml4(value);
    }

    /**
     * constructs a lucene query from query string and corpus parameters as set
     *
     * @param query
     * @param corpusIDs
     * @return
     */
    public static String queryBuilder(String query,
            Collection<String> corpusIDs) {
        String completeQuery; // holds original query and corpus
        // selection
        /**
         * find documents with metadataquery TODO: does not intercept with
         * parameters foundries and corpusIDs
         */

        /* add corpus ids to corpus query */
        StringBuilder corpusQuery = new StringBuilder("corpus:/(");
        for (String corpusId : corpusIDs) {
            corpusQuery.append(corpusId + "|");
        }
        corpusQuery.deleteCharAt(corpusQuery.length() - 1);
        corpusQuery.append(")/");
        completeQuery = "(" + query + ") AND " + corpusQuery.toString();
        jlog.debug("Searching documents matching '" + completeQuery + "'.");
        return completeQuery;
    }

}
