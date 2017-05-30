package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ids_mannheim.korap.utils.JsonUtils;

import java.util.*;

/**
 * @author hanl
 * @date 04/07/2015
 */
public abstract class KoralNode {
    private JsonNode node;
    private KoralRewriteBuilder rewrites;
    private boolean remove;


    private KoralNode (JsonNode node) {
        this.node = node;
        this.rewrites = new KoralRewriteBuilder();
        this.remove = false;
    }


    public static KoralNode wrapNode (JsonNode node) {
        return new KoralNode(node) {};
    }


    public void buildRewrites (JsonNode node) {
        this.rewrites.build(node);
    }


    public void buildRewrites () {
        this.rewrites.build(this.node);
    }


    @Override
    public String toString () {
        return this.node.toString();
    }


    public void put (String name, Object value) {
        if (this.node.isObject() && this.node.path(name).isMissingNode()) {
            ObjectNode node = (ObjectNode) this.node;
            if (value instanceof String)
                node.put(name, (String) value);
            else if (value instanceof Integer)
                node.put(name, (Integer) value);
            else if (value instanceof JsonNode)
                node.put(name, (JsonNode) value);
            this.rewrites.add("injection", name);
        }
        else
            throw new UnsupportedOperationException(
                    "node doesn't support this operation");
    }


    public void remove (Object identifier, RewriteIdentifier ident) {
        boolean set = false;
        if (this.node.isObject() && identifier instanceof String) {
            ObjectNode n = (ObjectNode) this.node;
            n.remove((String) identifier);
            set = true;
        }
        else if (this.node.isArray() && identifier instanceof Integer) {
            ArrayNode n = (ArrayNode) this.node;
            n.remove((Integer) identifier);
            set = true;
        }

        if (ident != null)
            identifier = ident.toString();

        if (set) {
            this.rewrites.add("deletion", identifier);
        }
    }


    public void replace (String name, Object value, RewriteIdentifier ident) {
        if (this.node.isObject() && this.node.has(name)) {
            ObjectNode n = (ObjectNode) this.node;
            if (value instanceof String)
                n.put(name, (String) value);
            else if (value instanceof Integer)
                n.put(name, (Integer) value);
            else if (value instanceof JsonNode)
                n.put(name, (JsonNode) value);

            if (ident != null)
                name = ident.toString();

            this.rewrites.add("override", name);
        }
    }
    
    public void replaceAt (String path, Object value, RewriteIdentifier ident) {
        if (this.node.isObject() && !this.node.at(path).isMissingNode()) {
            ObjectNode n = (ObjectNode) this.node.at(path);
            n.removeAll();
            n.putAll((ObjectNode)value);

            String name = path;
            if (ident != null)
                name = ident.toString();

            this.rewrites.add("override", name);
        }
    }

    public void set (String name, Object value, RewriteIdentifier ident) {
        if (this.node.isObject()) {
            ObjectNode n = (ObjectNode) this.node;
            if (value instanceof String)
                n.put(name, (String) value);
            else if (value instanceof Integer)
                n.put(name, (Integer) value);
            else if (value instanceof JsonNode)
                n.put(name, (JsonNode) value);


            if (ident != null)
                name = ident.toString();

            this.rewrites.add("insertion", name);
        }
    }


    public String get (String name) {
        if (this.node.isObject())
            return this.node.path(name).asText();
        return null;
    }


    public KoralNode at (String name) {
        this.node = this.node.at(name);
        return this;
    }


    public boolean has (Object ident) {
        if (ident instanceof String)
            return this.node.has((String) ident);
        else if (ident instanceof Integer)
            return this.node.has((int) ident);
        return false;
    }


    public JsonNode rawNode () {
        return this.node;
    }


    public void removeNode (RewriteIdentifier ident) {
        this.rewrites.add("deletion", ident.toString());
        this.remove = true;
    }

    public static class RewriteIdentifier {

        private String key, value;


        public RewriteIdentifier (String key, Object value) {
            this.key = key;
            this.value = value.toString();
        }


        @Override
        public String toString () {
            return key + "(" + value + ")";
        }


    }


    public boolean isRemove () {
        return this.remove;
    }


    public static class KoralRewriteBuilder {

        private List<KoralRewrite> rewrites;


        public KoralRewriteBuilder () {
            this.rewrites = new ArrayList<>();
        }


        public KoralRewriteBuilder add (String op, Object scope) {
            KoralRewrite rewrite = new KoralRewrite();
            rewrite.setOperation(op);
            rewrite.setScope(scope.toString());
            this.rewrites.add(rewrite);
            return this;
        }


        public JsonNode build (JsonNode node) {
            for (KoralRewrite rewrite : this.rewrites) {
                if (rewrite.map.get("operation") == null)
                    throw new UnsupportedOperationException(
                            "operation not set properly");

                if (node.has("rewrites")) {
                    ArrayNode n = (ArrayNode) node.path("rewrites");
                    n.add(JsonUtils.valueToTree(rewrite.map));
                }
                else if (node.isObject()) {
                    ObjectNode n = (ObjectNode) node;
                    List l = new LinkedList<>();
                    l.add(JsonUtils.valueToTree(rewrite.map));
                    n.put("rewrites", JsonUtils.valueToTree(l));
                }
                else {
                    //fixme: matches in result will land here. rewrites need to be placed under root node - though then there might be unclear where they belong to
                }

            }
            this.rewrites.clear();
            return node;
        }

    }



    private static class KoralRewrite {

        private Map<String, String> map;


        private KoralRewrite () {
            this.map = new LinkedHashMap<>();
            this.map.put("@type", "koral:rewrite");
            this.map.put("src", "Kustvakt");
        }


        public KoralRewrite setOperation (String op) {
            if (!op.startsWith("operation:"))
                op = "operation:" + op;
            this.map.put("operation", op);
            return this;
        }


        public KoralRewrite setScope (String scope) {
            this.map.put("scope", scope);
            return this;
        }

    }
}
