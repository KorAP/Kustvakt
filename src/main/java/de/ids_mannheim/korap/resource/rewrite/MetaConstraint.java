package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author hanl
 * @date 04/07/2015
 */
public class MetaConstraint extends RewriteQuery {

    public MetaConstraint() {
        super();
    }

    @Override
    public JsonNode rewrite(KoralNode node) {
        if (node.rawNode().has("meta")) {
            JsonNode meta = node.rawNode().path("meta");
            //todo: check meta parameter
            System.out.println("HAVE TO CHECK THE META ENTRIES");
        }
        return node.rawNode();
    }
}
