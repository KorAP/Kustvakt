package de.ids_mannheim.korap.rewrite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.QueryService;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.util.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * Rewrites virtual corpus reference with the corresponding koral
 * query describing the actual virtual corpus query.
 * 
 * @author margaretha
 *
 */
@Component
public class VirtualCorpusRewrite implements RewriteTask.RewriteQuery {

    @Autowired
    private KustvaktConfiguration config;
    @Autowired
    private QueryService queryService;

    @Override
    public KoralNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user, double apiVersion) throws KustvaktException {
    	if (node.has("corpus")) {
            node = node.at("/corpus");
            findVCRef(user.getUsername(), node);
        }
    	// EM: legacy
    	else if (node.has("collection")) {
            node = node.at("/collection");
            findVCRef(user.getUsername(), node);
        }
        return node;
    }

    private void findVCRef (String username, KoralNode koralNode)
            throws KustvaktException {
        if (koralNode.has("@type")
                && koralNode.get("@type").equals("koral:docGroupRef")) {
            if (!koralNode.has("ref")) {
                throw new KustvaktException(StatusCodes.MISSING_VC_REFERENCE,
                        "ref is not found");
            }
            else {
                String vcName = koralNode.get("ref");
                String vcOwner = "system";
                boolean ownerExist = false;
                if (vcName.contains("/")) {
                    String[] names = vcName.split("/");
                    if (names.length == 2) {
                        vcOwner = names[0];
                        vcName = names[1];
                        ownerExist = true;
                    }
                }

                String vcInCaching = config.getVcInCaching();
                if (vcName.equals(vcInCaching)) {
                    throw new KustvaktException(
                            de.ids_mannheim.korap.exceptions.StatusCodes.CACHING_VC,
                            "VC is currently busy and unaccessible due to "
                                    + "caching process",
                            koralNode.get("ref"));
                }

                QueryDO vc = queryService.searchQueryByName(username, vcName,
                        vcOwner, QueryType.VIRTUAL_CORPUS);
                if (!vc.isCached()) {
                    rewriteVC(vc, koralNode);
                }
                // required for named-vc since they are stored by filenames in the cache
                else if (ownerExist) {
                    removeOwner(vc.getKoralQuery(), vcOwner, koralNode);
                }
            }

        }
        else if (koralNode.has("operands")) {
            KoralNode operands = koralNode.at("/operands");

            for (int i = 0; i < operands.size(); i++) {
                KoralNode operand = operands.get(i);
                findVCRef(username, operand);
                operand.buildRewrites();
            }

        }
    }

    private void removeOwner (String koralQuery, String vcOwner,
            KoralNode koralNode) throws KustvaktException {
        JsonNode jsonNode = koralNode.rawNode();
        String ref = jsonNode.at("/ref").asText();
        String newRef = ref.substring(vcOwner.length() + 1, ref.length());
        koralNode.replace("ref", newRef, new RewriteIdentifier("ref", ref, 
        		"Ref has been replaced. The original value is described at "
        		+ "the original property."));
    }

    protected void rewriteVC (QueryDO vc, KoralNode koralNode)
            throws KustvaktException {
        String koralQuery = vc.getKoralQuery();
        JsonNode queryNode = JsonUtils.readTree(koralQuery);
        JsonNode newKoralQuery;
        if (queryNode.has("collection")) {
        	newKoralQuery = JsonUtils.readTree(koralQuery).at("/collection");
        }
        else {
        	newKoralQuery = JsonUtils.readTree(koralQuery).at("/corpus");
        }
        String source = koralNode.rawNode().toString();
        JsonNode sourceNode = JsonUtils.readTree(source);
        
		koralNode.replace(newKoralQuery, new RewriteIdentifier(null, sourceNode,
				"This node has been replaced. The original node is described at "
						+ "the original property."));
        
        // rewrite
//        koralNode.remove("@type", new RewriteIdentifier("@type", "",
//                jsonNode.at("/@type").asText()));
//        koralNode.remove("ref",
//                new RewriteIdentifier("ref", "", jsonNode.at("/ref").asText()));
//        koralNode.setAll((ObjectNode) kq);
    }
}
