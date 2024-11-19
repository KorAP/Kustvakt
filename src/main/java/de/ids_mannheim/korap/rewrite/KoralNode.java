package de.ids_mannheim.korap.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ids_mannheim.korap.utils.JsonUtils;

import java.util.*;

/**
 * @author hanl
 * @date 04/07/2015
 */
public class KoralNode {
    private JsonNode node;
    private KoralRewriteBuilder rewrites;
    private boolean remove;

    public KoralNode (JsonNode node) {
        this.node = node;
        this.rewrites = new KoralRewriteBuilder();
        this.remove = false;
    }

    public KoralNode (JsonNode node, KoralRewriteBuilder rewrites) {
        this.node = node;
        this.rewrites = rewrites;
        this.remove = false;
    }

    public static KoralNode wrapNode (JsonNode node) {
        return new KoralNode(node);
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

//            if (ident != null)
//                name = ident.toString();

            this.rewrites.add("override", null, ident);
        }
    }

    public void replaceAt (String path, Object value, RewriteIdentifier ident) {
        if (this.node.isObject() && 
                !this.node.at(path).isMissingNode()) {
            ObjectNode n = (ObjectNode) this.node.at(path);
            n.removeAll();
            n.putAll((ObjectNode) value);

            String name = path;
            if (ident != null)
                name = ident.toString(); // scope is simply RewriteIdentifier ?? 

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

    public void setAll (ObjectNode other) {
        if (this.node.isObject()) {
            ObjectNode n = (ObjectNode) this.node;
            n.setAll(other);
        }
        this.rewrites.add("insertion", null);
    }

    public String get (String name) {
        if (this.node.isObject())
            return this.node.path(name).asText();
        return null;
    }

    public KoralNode at (String name) {
        //        this.node = this.node.at(name);
        //        return this;
        return new KoralNode(this.node.at(name), this.rewrites);
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

        private String scope, value;
        private Object source;

        public RewriteIdentifier (String scope, String value) {
            this.scope = scope;
            this.value = value;
        }
        
        public RewriteIdentifier (String scope, String value, Object source) {
            this.scope = scope;
            this.value = value;
            this.source = source;
        }
        
        public String getScope () {
            return scope;
        }
        
        public Object getSource () {
            return source;
        }
        
        @Override
        public String toString () {
            return scope + "(" + value + ")";
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
            if (scope != null) {
                rewrite.setScope(scope.toString());
            }
            this.rewrites.add(rewrite);
            return this;
        }
        
        public KoralRewriteBuilder add (String op, String scope, RewriteIdentifier ri) {
            KoralRewrite rewrite = new KoralRewrite();
            rewrite.setOperation(op);
            if (ri.getScope() != null) {
                rewrite.setScope(ri.getScope());
            }
            if (ri.getSource() != null) {
                rewrite.setSource(ri.getSource());
            }
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

        private Map<String, Object> map;

        private KoralRewrite () {
            this.map = new LinkedHashMap<>();
            this.map.put("@type", "koral:rewrite");
            this.map.put("src", "Kustvakt");
            this.map.put("origin", "Kustvakt");
        }

        public void setOperation (String op) {
            if (!op.startsWith("operation:"))
                op = "operation:" + op;
            this.map.put("operation", op);
        }

        public void setScope (String scope) {
            this.map.put("scope", scope);
        }
        
        public void setSource(Object source) {
            this.map.put("source", source);
        }

    }

    public boolean isMissingNode (String string) {
        return this.node.at(string).isMissingNode();
    }

    public int size () {
        return this.node.size();
    }

    public KoralNode get (int i) {
        //        this.node = this.node.get(i);
        return this.wrapNode(this.node.get(i));
    }

    public int asInt() {
        return this.node.asInt();
    }
    
    public String asText(){
        return this.node.asText();
    }
}
