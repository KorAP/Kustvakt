package de.ids_mannheim.korap.rewrite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;

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
    
    private boolean cutContext (KoralNode context, String position) 
            throws KustvaktException {
        KoralNode contextPosition = context.at("/" + position);
        String type = contextPosition.at("/0").asText();

        if (type.equals("token")) {
            int length = contextPosition.at("/1").asInt();
            int maxContextLength = config.getMaxTokenContext();
            if (length > maxContextLength) {
                JsonNode sourceNode = JsonUtils
                        .readTree(contextPosition.toString());
                ArrayNode arrayNode = (ArrayNode) contextPosition.rawNode();
                arrayNode.set(1, maxContextLength);
				context.replace(position, arrayNode,
						new RewriteIdentifier(position, sourceNode, position
								+ " has been replaced. The original value is "
								+ "described in the source property."));
                return true;
            }
        }
        return false;
    }
}