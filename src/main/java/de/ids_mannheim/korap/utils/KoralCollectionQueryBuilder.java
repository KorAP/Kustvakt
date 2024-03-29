package de.ids_mannheim.korap.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.CollectionQueryProcessor;
import de.ids_mannheim.korap.response.Notifications;

/**
 * convenience builder class for collection query
 * 
 * @author hanl, margaretha
 * @date 29/06/2017
 */
public class KoralCollectionQueryBuilder {

    public enum EQ {
        EQUAL, UNEQUAL
    }

    private boolean verbose;
    private JsonNode base;
    private StringBuilder builder;
    private String mergeOperator;

    public KoralCollectionQueryBuilder () {
        this(false);
    }

    public KoralCollectionQueryBuilder (boolean verbose) {
        this.verbose = verbose;
        this.builder = new StringBuilder();
        this.base = null;
        this.mergeOperator = null;
    }

    /**
     * raw method for field - value pair adding. Supports all
     * operators (leq, geq, contains, etc.)
     * 
     * @param field
     * @param op
     * @param value
     * @return
     */
    public KoralCollectionQueryBuilder with (String field, String op,
            String value) {
        //String end = this.builder.substring(this.builder.length() - 4,
        //        this.builder.length() - 1);
        //if (this.builder.length() != 0
        //        && (!end.contains("&") | !end.contains("|")))
        //    throw new RuntimeException("no join operator given!");
        this.with(field + op + value);
        return this;
    }

    /**
     * element can be a more complex sub query like
     * (textClass=freizeit & Attributes.CORPUS_SIGLE=WPD)
     * 
     * @param query
     *            will be enclosed by parenthesis in order to make sub
     *            query
     *            element
     * @return
     */
    public KoralCollectionQueryBuilder with (String query) {
        if (!query.startsWith("(") && !query.endsWith(")"))
            query = "(" + query + ")";
        this.builder.append(query);
        return this;
    }

    public KoralCollectionQueryBuilder and () {
        if (this.builder.length() != 0)
            this.builder.append(" & ");
        if (this.base != null && this.mergeOperator == null)
            this.mergeOperator = "AND";
        return this;
    }

    public KoralCollectionQueryBuilder or () {
        if (this.builder.length() != 0)
            this.builder.append(" | ");
        if (this.base != null && this.mergeOperator == null)
            this.mergeOperator = "OR";
        return this;
    }

    public JsonNode rebaseCollection () throws KustvaktException {
        if (this.builder.length() == 0 && this.base == null)
            return null;

        JsonNode request = null;
        if (this.builder.length() != 0) {
            CollectionQueryProcessor tree = new CollectionQueryProcessor(
                    this.verbose);
            tree.process(this.builder.toString());
            if (tree.getErrors().size() > 0) {
                // legacy support
                for (List<Object> e : tree.getErrors()) {
                    if (e.get(1) instanceof String[]) {
                        Notifications notif = new Notifications();
                        int code = (int) e.get(0);
                        notif.addError(code, (String[]) e.get(1));
                        String notificationStr = notif.toJsonString();
                        throw new KustvaktException(
                                StatusCodes.SERIALIZATION_FAILED,
                                notificationStr, true);
                    }
                    else {
                        break;
                    }
                }
                // -- end of legacy support

                Map<String, Object> map = new HashMap<>();
                map.put("errors", tree.getErrors());
                String errors = JsonUtils.toJSON(map);
                throw new KustvaktException(StatusCodes.SERIALIZATION_FAILED,
                        errors, true);
            }
            request = JsonUtils.valueToTree(tree.getRequestMap());
        }

        if (this.base != null) {
            // check that collection non empty
            JsonNode tmp = this.base.deepCopy();
            if (request != null)
                request = mergeWith(request);
            else
                request = tmp;
        }
        return request;
    }

    public JsonNode mergeWith (JsonNode node) {
        if (this.base != null) {
            if (node != null) {
                JsonNode tobase = node.at("/collection");
                JsonNode base = this.base.deepCopy();
                JsonNode result = base.at("/collection");

                if (result.isMissingNode() && !tobase.isMissingNode())
                    result = tobase;
                else if (result.isMissingNode() && tobase.isMissingNode())
                    return base;
                else {
                    result = JsonBuilder
                            .buildDocGroup(this.mergeOperator != null
                                    ? this.mergeOperator.toLowerCase()
                                    : "and", result, tobase);
                }
                ((ObjectNode) base).put("collection", result);
                return base;
            }
            return this.base;
        }
        throw new RuntimeException("no query found to merge with!");
    }

    /**
     * sets base query. All consequent queries are added to the first
     * koral:docGroup within the collection base query
     * If no group in base query, consequent queries are skipped.
     * 
     * @param query
     * @throws KustvaktException
     */
    public KoralCollectionQueryBuilder setBaseQuery (String query)
            throws KustvaktException {
        this.base = JsonUtils.readTree(query);
        return this;
    }

    public KoralCollectionQueryBuilder setBaseQuery (JsonNode query) {
        this.base = query;
        return this;
    }

    public String toJSON () throws KustvaktException {
        return JsonUtils.toJSON(rebaseCollection());
    }

    @Override
    public String toString () {
        return this.builder.toString();
    }

    private static class JsonBuilder {

        public static ObjectNode buildDoc (String key, String value) {
            ObjectNode node = JsonUtils.createObjectNode();
            node.put("@type", "koral:doc");
            // eq.equals(EQ.EQUAL) ? "match:eq" : "match:ne"
            node.put("match", "match:eq");
            node.put("key", key);
            node.put("value", value);
            return node;
        }

        public static ObjectNode buildDocGroup (String op,
                JsonNode ... groups) {
            ObjectNode node = JsonUtils.createObjectNode();
            node.put("@type", "koral:docGroup");
            node.put("operation", "operation:" + op);
            ArrayNode ops = JsonUtils.createArrayNode();
            ops.addAll(Arrays.asList(groups));
            node.put("operands", ops);
            return node;
        }

    }

}
