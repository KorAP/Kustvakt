package de.ids_mannheim.korap.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanInjectable;
import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 25/09/2015
 */
public class IdWriter implements RewriteTask.RewriteKoralToken {

    private int counter;


    public IdWriter () {
        this.counter = 0;
    }


    @Override
    public KoralNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) {
        if (node.get("@type").equals("koral:token")) {
            String s = extractToken(node.rawNode());
            if (s != null && !s.isEmpty())
                node.put("idn", s + "_" + counter++);
        }
        return node;
    }


    private String extractToken (JsonNode token) {
        JsonNode wrap = token.path("wrap");
        if (!wrap.isMissingNode())
            return wrap.path("key").asText();
        return null;
    }
}
