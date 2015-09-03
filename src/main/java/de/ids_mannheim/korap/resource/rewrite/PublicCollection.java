package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author hanl
 * @date 04/07/2015
 */
public class PublicCollection extends RewriteQuery {

    @Override
    public JsonNode rewrite(KoralNode node) {
        JsonNode subnode = node.rawNode();
        if (!subnode.at("/collection").findValuesAsText("key")
                .contains("corpusID")) {
            //todo: inject public collection node
        }
        return subnode;
    }
}
