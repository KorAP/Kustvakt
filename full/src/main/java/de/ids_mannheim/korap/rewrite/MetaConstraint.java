package de.ids_mannheim.korap.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanInjectable;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 04/07/2015
 */
public class MetaConstraint implements RewriteTask.RewriteNodeAt {

    @Override
    public KoralNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) {
        // redundant
        if (node.rawNode().has("meta")) {
            JsonNode meta = node.rawNode().path("meta");
            //todo: check meta parameter
            System.out.println("HAVE TO CHECK THE META ENTRIES");
        }
        return node;
    }

    @Override
    public JsonNode rewriteResult (KoralNode node) {
        return null;
    }

    @Override
    public String at () {
        return "/meta";
    }

}
