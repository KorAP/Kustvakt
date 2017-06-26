package de.ids_mannheim.korap.resource.rewrite;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.object.KoralMatchOperator;
import de.ids_mannheim.korap.resource.rewrite.KoralNode.RewriteIdentifier;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;

/**
 * @author margaretha
 * @date 2 June 2017
 */
public class CollectionRewrite implements RewriteTask.RewriteQuery {

    private static Logger jlog = LoggerFactory
            .getLogger(CollectionRewrite.class);

    public CollectionRewrite () {
        super();
    }

    
    private List<String> checkAvailability (JsonNode node, List<String> userAvailabilities) {

        if (node.has("operands")) {
            ArrayList<JsonNode> operands = Lists
                    .newArrayList(node.at("/operands").elements());
            for (int i = 0; i < operands.size(); i++) {
                userAvailabilities = checkAvailability(operands.get(i), userAvailabilities);
            }
        }
        else if (node.has("key")
                && node.at("/key").asText().equals("availability")) {
            String queryAvailability = node.at("/value").asText();
            String matchOp = node.at("/match").asText();
            if (!userAvailabilities.contains(queryAvailability)){
                userAvailabilities.remove(queryAvailability);
            }
        }

        return userAvailabilities;
    }

    @Override
    public JsonNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) throws KustvaktException {
        JsonNode jsonNode = node.rawNode();
        
        List<String> userAvailabilities = new ArrayList<String>();
        switch (user.getCorpusAccess()) {
            case PUB:
                userAvailabilities.add("CC-BY.*");
                userAvailabilities.add("ACA.*");
//                builder.with(
//                        "availability = /CC-BY.*/ | availability = /ACA.*/");
                break;
            case ALL:
                userAvailabilities.add("CC-BY.*");
                userAvailabilities.add("ACA.*");
                userAvailabilities.add("QAO.*");

//                builder.with("availability = /QAO.*/ | availability = /ACA.*/ |"
//                        + "  availability = /CC-BY.*/");
                break;
            case FREE:
                userAvailabilities.add("CC-BY.*");
//                builder.with("availability   = /CC-BY.*/");
                break;
        }

        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
        RewriteIdentifier identifier = new KoralNode.RewriteIdentifier(
                Attributes.AVAILABILITY, user.getCorpusAccess());
        JsonNode rewrittesNode;

        if (jsonNode.has("collection")) {
            userAvailabilities = checkAvailability(jsonNode.at("/collection"), userAvailabilities);
            if (!userAvailabilities.isEmpty()){
                builder.with(buildAvailability(userAvailabilities));
            }
            builder.setBaseQuery(builder.toJSON());
            rewrittesNode = builder.mergeWith(jsonNode).at("/collection");
            node.set("collection", rewrittesNode, identifier);
        }
        else {
            builder.with(buildAvailability(userAvailabilities));
            rewrittesNode = JsonUtils.readTree(builder.toJSON())
                    .at("/collection");
            node.set("collection", rewrittesNode, identifier);
        }

        jlog.info("REWRITES: " + node.at("/collection").toString());
        return node.rawNode();
    }
    
    
    private String buildAvailability (List<String> userAvailabilities) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < userAvailabilities.size()-1; i++){
            sb.append("availability=/");
            sb.append(userAvailabilities.get(i));
            sb.append("/ | ");
        }
        sb.append("availability=/");
        sb.append(userAvailabilities.get(userAvailabilities.size()-1));
        sb.append("/");
        return sb.toString();
    }
}

