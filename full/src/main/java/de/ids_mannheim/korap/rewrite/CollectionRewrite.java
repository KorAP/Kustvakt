package de.ids_mannheim.korap.rewrite;


import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.object.KoralMatchOperator;
import de.ids_mannheim.korap.query.object.KoralOperation;
import de.ids_mannheim.korap.rewrite.KoralNode.RewriteIdentifier;
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

    public static Logger jlog =
            LogManager.getLogger(CollectionRewrite.class);

    public static boolean DEBUG = false;
    
    public CollectionRewrite () {
        super();
    }


    private List<String> checkAvailability (JsonNode node,
            List<String> originalAvailabilities,
            List<String> updatedAvailabilities, boolean isOperationOr) {
        if (DEBUG) {
            try {
                jlog.debug(JsonUtils.toJSON(node));
            }
            catch (KustvaktException e) {
                e.printStackTrace();
            }
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
                    node = operands.get(i);
                    if (node.has("key") && !node.at("/key").asText()
                            .equals("availability")) {
                        jlog.debug("RESET availabilities 1, key="
                                + node.at("/key").asText());
                        updatedAvailabilities.clear();
                        updatedAvailabilities.addAll(originalAvailabilities);
                        break;
                    }
                    else {
                        updatedAvailabilities = checkAvailability(
                                operands.get(i), originalAvailabilities,
                                updatedAvailabilities, true);
                    }
                }
            }
        }
        else if (node.has("key")
                && node.at("/key").asText().equals("availability")) {
            String queryAvailability = node.at("/value").asText();
            String matchOp = node.at("/match").asText();

            if (originalAvailabilities.contains(queryAvailability)
                    && matchOp.equals(KoralMatchOperator.EQUALS.toString())) {
                if (DEBUG) {
                    jlog.debug("REMOVE " + queryAvailability);
                }
                updatedAvailabilities.remove(queryAvailability);
            }
            else if (isOperationOr) {
                if (DEBUG) { 
                    jlog.debug("RESET availabilities 2");
                }
                updatedAvailabilities.clear();
                updatedAvailabilities.addAll(originalAvailabilities);
                return updatedAvailabilities;
            }
        }
        return updatedAvailabilities;
    }

    @Override
    public KoralNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) throws KustvaktException {
        JsonNode jsonNode = node.rawNode();

        FullConfiguration fullConfig = (FullConfiguration) config;

        List<String> userAvailabilities = new ArrayList<String>();
        switch (user.getCorpusAccess()) {
            case PUB:
                userAvailabilities.addAll(fullConfig.getFreeRegexList());
                userAvailabilities.addAll(fullConfig.getPublicRegexList());
                break;
            case ALL:
                userAvailabilities.addAll(fullConfig.getFreeRegexList());
                userAvailabilities.addAll(fullConfig.getPublicRegexList());
                userAvailabilities.addAll(fullConfig.getAllRegexList());
                break;
            case FREE:
                userAvailabilities.addAll(fullConfig.getFreeRegexList());
                break;
        }

        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
        RewriteIdentifier identifier = new KoralNode.RewriteIdentifier(
                Attributes.AVAILABILITY, user.getCorpusAccess());
        JsonNode rewrittenNode;

        if (jsonNode.has("collection")) {
            List<String> avalabilityCopy =
                    new ArrayList<String>(userAvailabilities.size());
            avalabilityCopy.addAll(userAvailabilities);
            if (DEBUG) {
                jlog.debug("Availabilities: "
                        + Arrays.toString(userAvailabilities.toArray()));
            }

            userAvailabilities = checkAvailability(jsonNode.at("/collection"),
                    avalabilityCopy, userAvailabilities, false);
            if (!userAvailabilities.isEmpty()) {
                builder.with(buildAvailability(avalabilityCopy));
                if (DEBUG) {
                    jlog.debug("corpus query: " + builder.toString());
                }
                builder.setBaseQuery(builder.toJSON());
                rewrittenNode = builder.mergeWith(jsonNode).at("/collection");
                node.set("collection", rewrittenNode, identifier);
            }
        }
        else {
            builder.with(buildAvailability(userAvailabilities));
            if (DEBUG) {
                jlog.debug("corpus query: " + builder.toString());
            }
            rewrittenNode =
                    JsonUtils.readTree(builder.toJSON()).at("/collection");
            node.set("collection", rewrittenNode, identifier);
        }

        node = node.at("/collection");
        if (DEBUG) { 
            jlog.debug("REWRITES: " + node.toString());
        }
        
        return node;
    }


    private String buildAvailability (List<String> userAvailabilities) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < userAvailabilities.size(); i++) {
            parseAvailability(sb, userAvailabilities.get(i), "|");
        }
        String availabilities = sb.toString();
        return availabilities.substring(0, availabilities.length() - 3);
    }

    private void parseAvailability (StringBuilder sb, String availability,
            String operator) {
        String uaArr[] = null;
        if (availability.contains("|")) {
            uaArr = availability.split("\\|");
            for (int j = 0; j < uaArr.length; j++) {
                parseAvailability(sb, uaArr[j].trim(), "|");
            }
        }
        // EM: not supported
        //        else if (availability.contains("&")){
        //            uaArr = availability.split("&");
        //            for (int j=0; j < uaArr.length -1; j++){
        //                parseAvailability(sb, uaArr[j], "&");
        //            }
        //            parseAvailability(sb, uaArr[uaArr.length-1], "|");
        //        } 
        else {
            sb.append("availability=/");
            sb.append(availability);
            sb.append("/ ");
            sb.append(operator);
            sb.append(" ");
        }

    }

}

