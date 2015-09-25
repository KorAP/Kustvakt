package de.ids_mannheim.korap.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ids_mannheim.korap.query.serialize.CollectionQueryProcessor;

/**
 * convenience builder class for collection query
 *
 * @author hanl
 * @date 16/09/2014
 */
public class CollectionQueryBuilder3 {

    public enum EQ {
        EQUAL, UNEQUAL
    }

    private boolean verbose;
    private JsonNode base;
    private StringBuilder builder;

    public CollectionQueryBuilder3() {
        this(false);
    }

    public CollectionQueryBuilder3(boolean verbose) {
        this.verbose = verbose;
        this.builder = new StringBuilder();
        this.base = null;
    }

    public CollectionQueryBuilder3 addSegment(String field, EQ eq,
            String value) {
        if (base == null)
            this.builder
                    .append(field + (eq.equals(EQ.EQUAL) ? "=" : "!=") + value);
        else {
            JsonNode node = Utils.buildDoc(field, value, eq);
            appendToBaseGroup(node);
        }
        return this;
    }

    /**
     * element can be a more complex sub query like (textClass=freizeit & corpusID=WPD)
     *
     * @param query will be parenthised in order to make sub query element
     * @return
     */
    public CollectionQueryBuilder3 addSub(String query) {
        if (!query.startsWith("(") && !query.endsWith(")"))
            query = "(" + query + ")";

        if (base != null) {
            CollectionQueryProcessor tree = new CollectionQueryProcessor(
                    this.verbose);
            tree.process(query);
            JsonNode map = JsonUtils
                    .valueToTree(tree.getRequestMap().get("collection"));
            appendToBaseGroup(map);
        }else
            this.builder.append(query);
        return this;
    }

    public CollectionQueryBuilder3 and() {
        this.builder.append(" & ");
        return this;
    }

    public CollectionQueryBuilder3 or() {
        this.builder.append(" | ");
        return this;
    }

    @Deprecated
    public CollectionQueryBuilder3 addRaw(String collection) {
        return this;
    }

    public Object getRequest() {
        Object request = base;
        if (request == null) {
            CollectionQueryProcessor tree = new CollectionQueryProcessor(
                    this.verbose);
            tree.process(this.builder.toString());

            request = tree.getRequestMap();
        }
        return request;
    }

    /**
     * sets base query. All consequent queries are added to the first koral:docGroup within the collection base query
     * If no group in base query, consequent queries are skipped.
     *
     * @param query
     */
    public void setBaseQuery(String query) {
        this.base = JsonUtils.readTree(query);
    }

    public String toJSON() {
        return JsonUtils.toJSON(getRequest());
    }



    private void appendToBaseGroup(JsonNode node) {
        if (base.at("/collection/@type").asText().equals("koral:docGroup")) {
            ArrayNode group = (ArrayNode) base.at("/collection/operands");
            if (node instanceof ArrayNode)
                group.addAll((ArrayNode) node);
            else
                group.add(node);
        }else
            throw new IllegalArgumentException("No group found to add to!");
        // fixme: if base is a doc only, this function is not supported. requirement is a koral:docGroup, since
        // combination operator is unknown otherwise
    }

    public static class Utils {

        public static JsonNode buildDoc(String key, String value, EQ eq) {
            ObjectNode node = JsonUtils.createObjectNode();
            node.put("@type", "koral:doc");
            node.put("match", eq.equals(EQ.EQUAL) ? "match:eq" : "match:ne");
            node.put("key", key);
            node.put("value", value);

            return node;
        }

        public static JsonNode buildDocGroup() {
            ObjectNode node = JsonUtils.createObjectNode();

            return node;
        }

    }

}
