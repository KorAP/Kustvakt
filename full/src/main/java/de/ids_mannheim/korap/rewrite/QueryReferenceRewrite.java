package de.ids_mannheim.korap.rewrite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.rewrite.KoralNode.RewriteIdentifier;
import de.ids_mannheim.korap.service.QueryService;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * Rewrites query reference with the corresponding koral
 * query describing the actual query fragment.
 * 
 * Based on VirtualCorpusRewrite. 
 *
 * @author diewald, margaretha
 *
 */
@Component
public class QueryReferenceRewrite implements RewriteTask.RewriteQuery {

    @Autowired
    private QueryService service;

    @Override
    public KoralNode rewriteQuery (KoralNode node,
                                   KustvaktConfiguration config,
                                   User user) throws KustvaktException {
        if (node.has("query")) {
            node = node.at("/query");
            findQueryRef(user.getUsername(), node);
        }
        return node;
    }

    private void findQueryRef (String username, KoralNode koralNode)
        throws KustvaktException {
        if (koralNode.has("@type")
            && koralNode.get("@type").equals("koral:queryRef")) {
            if (!koralNode.has("ref")) {
                throw new KustvaktException(
                    de.ids_mannheim.korap.util.StatusCodes.MISSING_QUERY_REFERENCE,
                    "ref is not found"
                    );
            }
            else {
                String queryRefName = koralNode.get("ref");
                String queryRefOwner = "system";
                if (queryRefName.contains("/")) {
                    String[] names = queryRefName.split("/");
                    if (names.length == 2) {
                        queryRefOwner = names[0];
                        queryRefName = names[1];
                    }
                }

                QueryDO qr = service.searchQueryByName(username,
                        queryRefName, queryRefOwner, QueryType.QUERY);

                if (qr == null) {
                    throw new KustvaktException(StatusCodes.NO_RESOURCE_FOUND,
                            "Query reference " + queryRefName
                                    + " is not found.",
                            String.valueOf(queryRefName));
                }

                // TODO:
                //   checkVCAcess(q, username);
                JsonNode qref = JsonUtils.readTree(qr.getKoralQuery());;
                rewriteQuery(qref,koralNode);
            }
        }
        
        else if (koralNode.has("operands")) {
            KoralNode operands = koralNode.at("/operands");
        
            for (int i = 0; i < operands.size(); i++) {
                KoralNode operand = operands.get(i);
                this.findQueryRef(username, operand);
                operand.buildRewrites();
            }
        }
    }


    private void rewriteQuery (JsonNode qref, KoralNode koralNode)
        throws KustvaktException {
        JsonNode jsonNode = koralNode.rawNode();
        koralNode.remove("@type",
                new RewriteIdentifier("@type", jsonNode.at("/@type").asText()));
        koralNode.remove("ref",
                new RewriteIdentifier("ref", jsonNode.at("/ref").asText()));
        koralNode.setAll((ObjectNode) qref);
    }
}
