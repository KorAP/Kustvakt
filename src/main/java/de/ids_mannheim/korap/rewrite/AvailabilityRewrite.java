package de.ids_mannheim.korap.rewrite;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.object.KoralMatchOperator;
import de.ids_mannheim.korap.query.object.KoralOperation;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;

/**
 * AvailabilityRewrite determines which availability field values are
 * possible for a user with respect to login and location of access.
 * 
 * <br/><br/>
 * KorAP differentiates 3 kinds of access:
 * <ul>
 * <li>FREE: without login</li>
 * <li>PUB: login outside IDS network</li>
 * <li>ALL: login within IDS network</li>
 * </ul>
 * 
 * Each of these accesses corresponds to a regular expression of
 * license formats defined in kustvakt.conf. For a given access, only those
 * resources whose availability field matches its regular expression
 * are allowed to be retrieved.
 * 
 * 
 * @author margaretha
 * @see CorpusAccess
 */
public class AvailabilityRewrite implements RewriteTask.RewriteQuery {

    public static Logger jlog = LogManager.getLogger(AvailabilityRewrite.class);
    
    public AvailabilityRewrite () {
        super();
    }

    private List<String> checkAvailability (JsonNode node,
    		List<String> availabilityRules,
    		List<String> actualAvailabilities, boolean isOperationOr) {

        if (node.has("operands")) {
            ArrayList<JsonNode> operands = Lists
                    .newArrayList(node.at("/operands").elements());

            if (node.at("/operation").asText()
                    .equals(KoralOperation.AND.toString())) {
                for (int i = 0; i < operands.size(); i++) {
                    actualAvailabilities = checkAvailability(operands.get(i),
                            availabilityRules, actualAvailabilities,
                            false);
                    if (actualAvailabilities.isEmpty())
                        break;
                }
            }
            else {
                for (int i = 0; i < operands.size(); i++) {
                    node = operands.get(i);
                    if (node.has("key") && !node.at("/key").asText()
                            .equals("availability")) {
                        jlog.debug("RESET availabilities 1, key="
                                + node.at("/key").asText());
                        actualAvailabilities.clear();
                        actualAvailabilities.addAll(availabilityRules);
                        break;
                    }
                    else {
                        actualAvailabilities = checkAvailability(
                                operands.get(i), availabilityRules,
                                actualAvailabilities, true);
                    }
                }
            }
        }
        else if (node.has("key")
                && node.at("/key").asText().equals("availability")) {
            String queryAvailability = node.at("/value").asText();
            String matchOp = node.at("/match").asText();

            if (availabilityRules.contains(queryAvailability)
                    && matchOp.equals(KoralMatchOperator.EQUALS.toString())) {
                actualAvailabilities.remove(queryAvailability);
            }
            else if (isOperationOr) {
                actualAvailabilities.clear();
                actualAvailabilities.addAll(availabilityRules);
                return actualAvailabilities;
            }
        }
        return actualAvailabilities;
    }

	@Override
	public KoralNode rewriteQuery (KoralNode koralNode,
			KustvaktConfiguration config, User user, double apiVersion) 
			throws KustvaktException {
		JsonNode jsonNode = koralNode.rawNode();

        FullConfiguration fullConfig = (FullConfiguration) config;
        CorpusAccess corpusAccess = user.getCorpusAccess();
        String corpusAccessName = user.accesstoString();
		List<String> availabilityRules = getAvailabilityRules(corpusAccess,
				fullConfig);

        String availabilityQuery = getCorpusQuery(corpusAccess, fullConfig);
        
        String collectionNodeName = (apiVersion >= 1.1) ? "corpus"
				: "collection";
        
        if (jsonNode.has(collectionNodeName)) {
            if (jsonNode.toString().contains("availability")) {
            	List<String> actualAvalability = new ArrayList<>();
                actualAvalability.addAll(availabilityRules);

				actualAvalability = checkAvailability(
						jsonNode.at("/" + collectionNodeName),
						availabilityRules, actualAvalability, false);
				if (!actualAvalability.isEmpty()) {
					createOperationAnd(availabilityQuery, jsonNode,
							corpusAccessName, koralNode, apiVersion,
							collectionNodeName);
                	
//                    builder.with(availabilityQuery);
//                    builder.setBaseQuery(builder.toJSON());
//                    rewrittenNode = builder.mergeWith(jsonNode).at("/collection");
//                    koralNode.set("collection", rewrittenNode, identifier);
                }
			}
			else {
				createOperationAnd(availabilityQuery, jsonNode,
						corpusAccessName, koralNode, apiVersion,
						collectionNodeName);
			}
        }
		else {
			KoralCollectionQueryBuilder builder = 
					new KoralCollectionQueryBuilder(apiVersion);
			builder.with(availabilityQuery);
			JsonNode rewrittenNode = JsonUtils.readTree(builder.toJSON())
					.at("/"+collectionNodeName);
			
			RewriteIdentifier identifier = new RewriteIdentifier(null, null,
					corpusAccessName + " corpus access policy has been added.");
			koralNode.set(collectionNodeName, rewrittenNode, identifier);
		}

        koralNode = koralNode.at("/"+collectionNodeName);
        return koralNode;
    }
    
    private void createOperationAnd (String availabilityQuery,
			JsonNode jsonNode, String corpusAccessName, KoralNode node,
			double apiVersion, String collectionNodeName)
			throws KustvaktException {

		KoralCollectionQueryBuilder availabilityBuilder = 
				new KoralCollectionQueryBuilder(apiVersion);
		availabilityBuilder.with(availabilityQuery);
		JsonNode availabilityNode = JsonUtils
				.readTree(availabilityBuilder.toJSON());

		String source = jsonNode.at("/"+collectionNodeName).toString();
		JsonNode sourceNode = JsonUtils.readTree(source);

		KoralCollectionQueryBuilder builder = 
				new KoralCollectionQueryBuilder(apiVersion);
		// Base query must contains collection or corpus node
		builder.setBaseQuery(availabilityNode);
		JsonNode rewrittenNode = builder.mergeWith(jsonNode)
				.at("/" + collectionNodeName);
		RewriteIdentifier identifier = new RewriteIdentifier(null, sourceNode,
				corpusAccessName + " corpus access policy has been added.");
		node.replace(collectionNodeName, rewrittenNode, identifier);
	}
    
    private List<String> getAvailabilityRules (CorpusAccess access,
			FullConfiguration fullConfig) {
		switch (access) {
			case PUB:
				return fullConfig.getPublicRegexList();
			case ALL:
				return fullConfig.getAllRegexList();
			default: // free
				return fullConfig.getFreeRegexList();
		}
	}

	private String getCorpusQuery (CorpusAccess access,
			FullConfiguration fullConfig) {
		switch (access) {
			case PUB:
				return fullConfig.getPublicAvailabilityQuery();
			case ALL:
				return fullConfig.getAllAvailabilityQuery();
			default: // free
				return fullConfig.getFreeAvailabilityQuery();
		}

	}
}
