package de.ids_mannheim.korap.rewrite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.dao.UserGroupDao;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.service.UserGroupService;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;

@Component
public class QueryContextRewrite implements RewriteTask.RewriteQuery {

    private static final String LARGE_CONTEXT_GROUP = "LargeContextGroup";

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private UserGroupDao userGroupDao;

    @Override
    public KoralNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user, double apiVersion) throws KustvaktException {

        boolean useLargeContext = config.isLargeContextGroupEnabled()
                && isInLargeContextGroup(user);
        int maxContext = useLargeContext
                ? config.getMaxTokenContextLarge()
                : config.getMaxTokenContext();
        if (maxContext > 0) {
            boolean isContextCut = false;
            KoralNode context = node.at("/meta/context");
            isContextCut = cutContext(context, "left", maxContext);
            isContextCut = cutContext(context, "right", maxContext) || isContextCut;
            if (isContextCut) context.buildRewrites();
        }
        return node;
    }

    private boolean isInLargeContextGroup (User user)
            throws KustvaktException {
        if (user == null) return false;
        try {
        	UserGroup group = userGroupDao.retrieveGroupByName(LARGE_CONTEXT_GROUP,
                false);
        	return userGroupService.isMember(user.getUsername(), group);
        	
        }catch (KustvaktException e) {
			if (e.getStatusCode() == StatusCodes.NO_RESOURCE_FOUND) {
				return false;
			}
			throw e;
		}
    }
    
    private boolean cutContext (KoralNode context, String position,
            int maxContextLength)
            throws KustvaktException {
        KoralNode contextPosition = context.at("/" + position);
        String type = contextPosition.at("/0").asText();

        if (type.equals("token")) {
            int length = contextPosition.at("/1").asInt();
            if (length > maxContextLength) {
                JsonNode sourceNode = JsonUtils
                        .readTree(contextPosition.toString());
                ArrayNode arrayNode = (ArrayNode) contextPosition.rawNode();
                arrayNode.set(1, maxContextLength);
				context.replace(position, arrayNode,
						new RewriteIdentifier(position, sourceNode, position
								+ " has been replaced. The original value is "
								+ "described in the original property."));
                return true;
            }
        }
        return false;
    }
}