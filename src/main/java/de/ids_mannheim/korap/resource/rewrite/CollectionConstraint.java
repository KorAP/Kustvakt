package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 03/07/2015
 */
public class CollectionConstraint extends RewriteNode {

    @Override
    public JsonNode rewrite(KoralNode koralnode) {
        JsonNode node = koralnode.rawNode();
        if (node.at("/@type").asText().equals("koral:doc")) {
            if (node.at("/key").asText().equals("corpusID") && !check(node,
                    koralnode.getUser())) {
                koralnode.removeNode();
                // todo: add message that node was removed!
            }
        }
        return node;
    }

    private boolean check(JsonNode node, User user) {
        return false;
    }

}
