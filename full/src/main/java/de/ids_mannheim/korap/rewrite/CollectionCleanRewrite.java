package de.ids_mannheim.korap.rewrite;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;
import edu.emory.mathcs.backport.java.util.Arrays;

/** EM: not used anymore. This rewrite was to remove an empty koral:doc group in operands.
 * 
 * @author hanl
 * @date 28/07/2015
 */
public class CollectionCleanRewrite implements RewriteTask.RewriteNodeAt {

    @Override
    public KoralNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) {
        JsonNode jsonNode = process(node.rawNode());
        return node.wrapNode(jsonNode);
    }


    private JsonNode process (JsonNode root) {
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
                // remove group element and replace with single doc
                if (count == 1)
                    sub = node.path(0);
                // indicate empty group
                else if (count == 0) // can't do anything here -- fixme: edge case?!
                    return null;
            }

            // what happens to array nodes?
            if (!root.equals(sub)) {
                if (sub.isObject()) {
                    ObjectNode ob = (ObjectNode) root;
                    ob.remove(Arrays.asList(new String[] { "@type",
                            "operation", "operands" }));
                    ob.putAll((ObjectNode) sub);
                }
            }
        }
        return root;
    }


    @Override
    public JsonNode rewriteResult (KoralNode node) {
        return null;
    }


    @Override
    public String at () {
        return "/collection";
    }
}
