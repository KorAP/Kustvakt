package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;

import java.util.Iterator;

/**
 * @author hanl
 * @date 28/07/2015
 */

public class CollectionCleanupFilter implements RewriteTask.RewriteQuery {

    // track path to operand
    @Deprecated
    private StringBuilder builder;

    public CollectionCleanupFilter() {
        super();
    }

    @Override
    public JsonNode preProcess(KoralNode node, KustvaktConfiguration config,
            User user) {
        if (node.has("collection")) {
            JsonNode coll = node.rawNode().path("collection");
            process(coll);
        }
        return null;
    }

    private JsonNode process(JsonNode root) {
        JsonNode sub = root;
        if (root.isObject()) {
            if (root.has("operands")) {
                JsonNode node = root.at("/operands");
                Iterator<JsonNode> it = node.elements();
                while (it.hasNext()) {
                    JsonNode n = it.next();
                    JsonNode s = process(n);
                    if (s == null)
                        it.remove();
                }

                int count = node.size();
                if (count == 1)
                    sub = node.path(0);
                else if (count
                        == 0) // can't do anything here -- fixme: edge case?!
                    return null;
            }

            if (!root.equals(sub)) {
                if (sub.isObject()) {
                    ObjectNode ob = (ObjectNode) root;
                    ob.removeAll();
                    ob.putAll((ObjectNode) sub);
                }
            }
        }
        return root;
    }

    // return null deletes node, if node return replace at level -1
    @Deprecated
    private JsonNode processNodes(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            if (jsonNode.has("operands")) {
                JsonNode node = jsonNode.at("/operands");
                int count = node.size();
                if (count == 1) {
                    // move to super node if any
                    return node.path(0);
                }else if (count == 0) {
                    // remove container
                    return null;
                }
                return jsonNode;
            }
        }
        return JsonUtils.createArrayNode();
    }

    @Override
    public JsonNode postProcess(KoralNode node) {
        return null;
    }
}
