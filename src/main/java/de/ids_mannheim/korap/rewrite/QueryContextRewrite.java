package de.ids_mannheim.korap.rewrite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ArrayNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.rewrite.KoralNode.RewriteIdentifier;
import de.ids_mannheim.korap.user.User;

@Component
public class QueryContextRewrite implements RewriteTask.RewriteQuery {

    @Autowired
    private KustvaktConfiguration config;

    @Override
    public KoralNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) throws KustvaktException {
        
        if (config.getMaxTokenContext() > 0) {
            boolean isContextCut = false;
            KoralNode context = node.at("/meta/context");
            isContextCut = cutContext(context, "left");
            isContextCut = cutContext(context, "right") || isContextCut;
            if (isContextCut) context.buildRewrites();
        }
        return node;
    }
    
    private boolean cutContext (KoralNode context, String position) {
        KoralNode contextPosition = context.at("/" + position);
        String type = contextPosition.at("/0").asText();

        if (type.equals("token")) {
            int length = contextPosition.at("/1").asInt();
            int maxContextLength = config.getMaxTokenContext();
            if (length > maxContextLength) {
                ArrayNode arrayNode = (ArrayNode) contextPosition.rawNode();
                arrayNode.set(1, maxContextLength);
                context.replace(position, arrayNode, new RewriteIdentifier(
                        position, length));
                return true;
            }
        }
        return false;
    }
}