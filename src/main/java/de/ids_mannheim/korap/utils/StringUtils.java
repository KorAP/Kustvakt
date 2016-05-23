package de.ids_mannheim.korap.utils;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StringUtils {
    private final static Logger jlog = LoggerFactory
            .getLogger(StringUtils.class);

    private static final String SEP = ";";


    public static Collection<UUID> stringToUUIDList (String s) {
        String[] array = s.split(SEP);
        List<UUID> list = new LinkedList<>();
        for (String att : array) {
            list.add(UUID.fromString(att));
        }
        return list;
    }


    public static List<String> toList (String values) {
        List<String> list = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(values, SEP);
        while (tokenizer.hasMoreTokens())
            list.add(tokenizer.nextToken());
        return list;
    }


    public static Set<String> toSet (String values, String sep) {
        Set<String> set = new HashSet<>();
        if (values != null && !values.isEmpty()) {
            StringTokenizer tokenizer = new StringTokenizer(values, sep);
            while (tokenizer.hasMoreTokens())
                set.add(tokenizer.nextToken());
        }
        return set;
    }


    public static Set<String> toSet (String values) {
        return toSet(values, SEP);
    }


    public static String toString (Collection<String> values) {
        return StringUtils.toString(values, SEP);
    }


    public static String toString (Collection<String> values, String sep) {
        StringBuffer b = new StringBuffer();
        for (String s : values)
            b.append(s).append(sep);

        if (b.length() > 0)
            b.deleteCharAt(b.length() - 1);
        return b.toString();
    }


    public static String orderedToString (Collection<String> hash) {
        Set<String> orderedSet = new TreeSet<>();
        orderedSet.addAll(hash);
        if (orderedSet.isEmpty()) {
            return "";
        }
        else {
            StringBuilder builder = new StringBuilder();
            for (String s : orderedSet) {
                builder.append(s);
                builder.append(SEP);
            }
            builder.deleteCharAt(builder.length() - 1);
            return builder.toString();
        }
    }


    public static String UUIDsetToString (Collection<UUID> hash) {
        Set<UUID> orderedSet = new TreeSet<>();
        orderedSet.addAll(hash);
        if (orderedSet.isEmpty()) {
            return "";
        }
        else {
            StringBuilder builder = new StringBuilder();
            for (UUID s : orderedSet) {
                builder.append(s);
                builder.append(SEP);
            }
            builder.deleteCharAt(builder.length() - 1);
            return builder.toString();
        }
    }


    public static String buildSQLRegex (String path) {
        StringBuilder b = new StringBuilder();
        String[] match = path.split("/");
        b.append(match[0]);
        b.append("(" + "/" + match[1] + ")");
        b.append("*$");
        return b.toString();
    }


    public static boolean isInteger (String value) {
        try {
            Integer.valueOf(value);
            return true;
        }
        catch (IllegalArgumentException e) {
            // do nothing!
            return false;
        }
    }


    public static String normalize (String value) {
        return value.trim().toLowerCase();
    }


    public static String normalizeHTML (String value) {
        return StringEscapeUtils.escapeHtml(value);
    }


    public static String decodeHTML (String value) {
        return StringEscapeUtils.unescapeHtml(value);
    }


    /**
     * constructs a lucene query from query string and corpus
     * parameters as set
     * 
     * @param query
     * @param corpusIDs
     * @return
     */
    public static String queryBuilder (String query,
            Collection<String> corpusIDs) {
        String completeQuery; // holds original query and corpus
        // selection
        /**
         * find documents with metadataquery TODO: does not intercept
         * with
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
