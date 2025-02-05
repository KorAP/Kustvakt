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

        if (set) {
            this.rewrites.add("deletion", ident);
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

            this.rewrites.add("override", ident);
        }
    }
    
    public void replace (Object value, RewriteIdentifier ident) {
        ObjectNode n = (ObjectNode) this.node;
        if (value instanceof ObjectNode) {
            n.removeAll();
            n.setAll((ObjectNode) value);
            this.rewrites.add("override", ident);
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
    
    // Please use set instead.
    @Deprecated
    public void put (String name, Object value) {
        if (this.node.isObject() && this.node.path(name).isMissingNode()) {
            ObjectNode node = (ObjectNode) this.node;
            if (value instanceof String)
                node.put(name, (String) value);
            else if (value instanceof Integer)
                node.put(name, (Integer) value);
            else if (value instanceof JsonNode)
                node.set(name, (JsonNode) value);
            this.rewrites.add("injection", name);
        }
        else
            throw new UnsupportedOperationException(
                    "node doesn't support this operation");
    }

    // EM: we agree to use injection instead because it has been introduced to
    // public in several occasions.
    // This method is similar to put
    /**
     * @param name 	the key name of the node / JSON property to set
     * @param value	the value of the JSON property to set
     * @param ident RewriteIdentifier
     */
    public void set (String name, Object value, RewriteIdentifier ident) {
        if (this.node.isObject()) {
            ObjectNode n = (ObjectNode) this.node;
            if (value instanceof String)
                n.put(name, (String) value);
            else if (value instanceof Integer)
                n.put(name, (Integer) value);
            else if (value instanceof JsonNode)
                n.set(name, (JsonNode) value);

            this.rewrites.add("injection", ident);
        }
    }

    public void setAll (ObjectNode other) {
        if (this.node.isObject()) {
            ObjectNode n = (ObjectNode) this.node;
            n.setAll(other);
        }
        this.rewrites.add("injection", null);
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

    
    public boolean isRemove () {
        return this.remove;
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
