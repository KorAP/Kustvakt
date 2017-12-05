package de.ids_mannheim.korap.rewrite;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.object.KoralMatchOperator;
import de.ids_mannheim.korap.query.object.KoralOperation;
import de.ids_mannheim.korap.resource.rewrite.KoralNode;
import de.ids_mannheim.korap.resource.rewrite.KoralNode.RewriteIdentifier;
import de.ids_mannheim.korap.resource.rewrite.RewriteTask;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import edu.emory.mathcs.backport.java.util.Arrays;

/** CollectionRewrite determines which availability field values are 
 *  possible for a user with respect to his mean and location of access.
 *  
 *  <br/><br/>
 *  KorAP differentiates 3 kinds of access:
 *  <ul>
 *      <li>FREE: without login</li> 
 *      <li>PUB: login outside IDS network</li> 
 *      <li>ALL: login within IDS network</li> 
 *  </ul>
 *  
 *  Each of these accesses corresponds to a regular expression of license 
 *  formats defined in kustvakt.conf. For a given access, only those 
 *  resources whose availability field matches its regular expression 
 *  are allowed to be retrieved. 
 *  
 *  
 * @author margaretha
 * @last-update 21 Nov 2017
 * @see CorpusAccess
 */
public class CollectionRewrite implements RewriteTask.RewriteQuery {

    private static Logger jlog =
            LoggerFactory.getLogger(CollectionRewrite.class);

    public CollectionRewrite () {
        super();
    }


    private List<String> checkAvailability (JsonNode node,
            List<String> originalAvailabilities,
            List<String> updatedAvailabilities, boolean isOperationOr) {
        try {
            jlog.debug(JsonUtils.toJSON(node));
        }
        catch (KustvaktException e) {
            e.printStackTrace();
        }

        if (node.has("operands")) {
            ArrayList<JsonNode> operands =
                    Lists.newArrayList(node.at("/operands").elements());

            if (node.at("/operation").asText()
                    .equals(KoralOperation.AND.toString())) {
                for (int i = 0; i < operands.size(); i++) {
                    updatedAvailabilities = checkAvailability(operands.get(i),
                            originalAvailabilities, updatedAvailabilities,
                            false);
                    if (updatedAvailabilities.isEmpty()) break;
                }
            }
            else {
                for (int i = 0; i < operands.size(); i++) {
                    updatedAvailabilities = checkAvailability(operands.get(i),
                            originalAvailabilities, updatedAvailabilities,
                            true);
                }
            }
        }
        else if (node.has("key")
                && node.at("/key").asText().equals("availability")) {
            String queryAvailability = node.at("/value").asText();
            String matchOp = node.at("/match").asText();
            if (originalAvailabilities.contains(queryAvailability)
                    && matchOp.equals(KoralMatchOperator.EQUALS.toString())) {
                jlog.debug("REMOVE " + queryAvailability);
                updatedAvailabilities.remove(queryAvailability);
            }
            else if (isOperationOr) {
                jlog.debug("RESET availabilities");
                updatedAvailabilities.clear();
                updatedAvailabilities.addAll(originalAvailabilities);
                return updatedAvailabilities;
            }
        }
        return updatedAvailabilities;
    }

    @Override
    public JsonNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) throws KustvaktException {
        JsonNode jsonNode = node.rawNode();
        
        FullConfiguration fullConfig = (FullConfiguration) config;

        List<String> userAvailabilities = new ArrayList<String>();
        switch (user.getCorpusAccess()) {
            case PUB:
                userAvailabilities.add(fullConfig.getFreeOnlyRegex());
                userAvailabilities.add(fullConfig.getPublicOnlyRegex());
                break;
            case ALL:
                userAvailabilities.add(fullConfig.getFreeOnlyRegex());
                userAvailabilities.add(fullConfig.getPublicOnlyRegex());
                userAvailabilities.add(fullConfig.getAllOnlyRegex());
                break;
            case FREE:
                userAvailabilities.add(fullConfig.getFreeOnlyRegex());
                break;
        }

        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
        RewriteIdentifier identifier = new KoralNode.RewriteIdentifier(
                Attributes.AVAILABILITY, user.getCorpusAccess());
        JsonNode rewrittesNode;

        if (jsonNode.has("collection")) {
            List<String> avalabilityCopy =
                    new ArrayList<String>(userAvailabilities.size());
            avalabilityCopy.addAll(userAvailabilities);
            jlog.debug("Availabilities: "
                    + Arrays.toString(userAvailabilities.toArray()));

            userAvailabilities = checkAvailability(jsonNode.at("/collection"),
                    avalabilityCopy, userAvailabilities, false);
            if (!userAvailabilities.isEmpty()) {
                builder.with(buildAvailability(avalabilityCopy));
                builder.setBaseQuery(builder.toJSON());
                rewrittesNode = builder.mergeWith(jsonNode).at("/collection");
                node.set("collection", rewrittesNode, identifier);
            }
        }
        else {
            builder.with(buildAvailability(userAvailabilities));
            rewrittesNode =
                    JsonUtils.readTree(builder.toJSON()).at("/collection");
            node.set("collection", rewrittesNode, identifier);
        }

        jlog.info("REWRITES: " + node.at("/collection").toString());
        return node.rawNode();
    }


    private String buildAvailability (List<String> userAvailabilities) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < userAvailabilities.size() - 1; i++) {
            sb.append("availability=/");
            sb.append(userAvailabilities.get(i));
            sb.append("/ | ");
        }
        sb.append("availability=/");
        sb.append(userAvailabilities.get(userAvailabilities.size() - 1));
        sb.append("/");
        return sb.toString();
    }
}

