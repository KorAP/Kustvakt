package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author hanl
 * @date 04/07/2015
 */
public abstract class KoralNode {
    private JsonNode node;
    private KoralRewriteBuilder builder;
    private boolean toRemove;
    private final User user;

    private KoralNode(JsonNode node) {
        this(node, null);
    }

    public KoralNode(JsonNode node, User user) {
        this.node = node;
        this.builder = new KoralRewriteBuilder();
        this.toRemove = false;
        this.user = user;
    }

    public boolean hasUser() {
        return this.user != null;
    }

    public User getUser() {
        return this.user;
    }

    public static KoralNode getNode(JsonNode node) {
        return new KoralNode(node) {
        };
    }

    public void set(String name, Object value) {

        if (this.node.isObject()) {
            ObjectNode node = (ObjectNode) this.node;
            if (value instanceof String)
                node.put(name, (String) value);
            else if (value instanceof Integer)
                node.put(name, (Integer) value);
            builder.setOperation("injection");
            builder.build(this.node);
        }else
            throw new UnsupportedOperationException(
                    "node doesn't support this operation");
    }

    public void remove(Object identifier) {
        boolean set = false;
        if (this.node.isObject() && identifier instanceof String) {
            ObjectNode n = (ObjectNode) this.node;
            n.remove((String) identifier);
            set = true;
        }else if (this.node.isArray() && identifier instanceof Integer) {
            ArrayNode n = (ArrayNode) this.node;
            n.remove((Integer) identifier);
            set = true;
        }
        if (set) {
            builder.setOperation("deletion");
            builder.build(this.node);
        }
    }

    public void replace(String name, String value) {
        if (this.node.isObject() && this.node.has(name)) {
            ObjectNode n = (ObjectNode) this.node;
            n.put(name, value);
            builder.setOperation("override");
            builder.build(this.node);
        }
    }

    public JsonNode rawNode() {
        return this.node;
    }

    public void removeNode() {
        this.toRemove = true;
    }

    public boolean toRemove() {
        return this.toRemove;
    }

    private static class KoralRewriteBuilder {

        private Map<String, String> map;

        public KoralRewriteBuilder() {
            this.map = new LinkedHashMap<>();
            this.map.put("@type", "koral:rewrite");
            this.map.put("src", "Kustvakt");
        }

        public KoralRewriteBuilder setOperation(String op) {
            if (!op.startsWith("operation:"))
                op = "operation:" + op;
            this.map.put("operation", op);
            return this;
        }

        public KoralRewriteBuilder setScope(String scope) {
            this.map.put("scope", scope);
            return this;
        }

        public JsonNode build(JsonNode node) {
            if (this.map.get("operation") == null)
                throw new UnsupportedOperationException(
                        "operation not set properly");

            if (node.has("rewrites")) {
                ArrayNode n = (ArrayNode) node.path("rewrites");
                n.add(JsonUtils.valueToTree(this.map));
            }else {
                ObjectNode n = (ObjectNode) node;
                List l = new LinkedList<>();
                l.add(JsonUtils.valueToTree(this.map));
                n.put("rewrites", JsonUtils.valueToTree(l));
            }
            return node;
        }

    }
}




