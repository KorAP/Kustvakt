package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanInjectable;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 04/07/2015
 */
public class MetaConstraint
        implements RewriteTask.RewriteNodeAt {


    @Override
    public JsonNode preProcess(KoralNode node, KustvaktConfiguration config,
            User user) {
        // redundant
        if (node.rawNode().has("meta")) {
            JsonNode meta = node.rawNode().path("meta");
            //todo: check meta parameter
            System.out.println("HAVE TO CHECK THE META ENTRIES");
        }
        return node.rawNode();
    }

    @Override
    public JsonNode postProcess(KoralNode node) {
        return null;
    }


    @Override
    public String at() {
        return "/meta";
    }

}
