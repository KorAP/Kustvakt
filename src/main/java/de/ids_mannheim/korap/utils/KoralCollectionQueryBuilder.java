package de.ids_mannheim.korap.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ids_mannheim.korap.query.serialize.CollectionQueryProcessor;
import edu.emory.mathcs.backport.java.util.Arrays;

import java.util.Map;

/**
 * convenience builder class for collection query
 * 
 * @author hanl
 * @date 16/09/2014
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
    public KoralCollectionQueryBuilder fieldValue (String field, String op,
            String value) {
        this.builder.append(field + op + value);
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


    public Object rebaseCollection () {
        if (this.builder.length() == 0 && this.base == null)
            return null;

        JsonNode request = null;
        if (this.builder.length() != 0) {
            CollectionQueryProcessor tree = new CollectionQueryProcessor(
                    this.verbose);
            tree.process(this.builder.toString());
            request = JsonUtils.valueToTree(tree.getRequestMap());
        }

        if (this.base != null) {
            // check that collection non empty
            JsonNode tmp = this.base.deepCopy();
            if (request != null) {
                JsonNode tobase = request.at("/collection");
                request = tmp;
                JsonNode result = JsonBuilder.buildDocGroup(
                        this.mergeOperator != null ? this.mergeOperator
                                .toLowerCase() : "and", request
                                .at("/collection"), tobase);
                ((ObjectNode) request).put("collection", result);
            }
            else
                request = tmp;
        }
        return request;
    }


    public JsonNode mergeWith (JsonNode node) {
        if (this.base != null) {
            // check that collection non empty
            if (node != null) {
                JsonNode tobase = node.at("/collection");
                JsonNode base = this.base.deepCopy();
                JsonNode result = JsonBuilder.buildDocGroup("and",
                        base.at("/collection"), tobase);
                ((ObjectNode) base).put("collection", result);
                return base;
            }
            return null;
        }
        return null;
    }


    /**
     * sets base query. All consequent queries are added to the first
     * koral:docGroup within the collection base query
     * If no group in base query, consequent queries are skipped.
     * 
     * @param query
     */
    public KoralCollectionQueryBuilder setBaseQuery (String query) {
        this.base = JsonUtils.readTree(query);
        return this;
    }


    public KoralCollectionQueryBuilder setBaseQuery (JsonNode query) {
        this.base = query;
        return this;
    }


    public String toJSON () {
        return JsonUtils.toJSON(rebaseCollection());
    }


    @Override
    public String toString () {
        return this.builder.toString();
    }


    @Deprecated
    private KoralCollectionQueryBuilder appendToBaseGroup (JsonNode node) {
        if (base.at("/collection/@type").asText().equals("koral:docGroup")) {
            ArrayNode group = (ArrayNode) base.at("/collection/operands");
            if (node instanceof ArrayNode)
                group.addAll((ArrayNode) node);
            else
                group.add(node);
        }
        else
            throw new IllegalArgumentException("No group found to add to!");
        // fixme: if base is a doc only, this function is not supported. requirement is a koral:docGroup, since
        // combination operator is unknown otherwise
        return this;
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


        public static ObjectNode buildDocGroup (String op, JsonNode ... groups) {
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
