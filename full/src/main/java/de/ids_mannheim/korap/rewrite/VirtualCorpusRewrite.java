package de.ids_mannheim.korap.rewrite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resource.rewrite.KoralNode;
import de.ids_mannheim.korap.resource.rewrite.KoralNode.RewriteIdentifier;
import de.ids_mannheim.korap.resource.rewrite.RewriteTask;
import de.ids_mannheim.korap.service.VirtualCorpusService;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.util.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

@Component
public class VirtualCorpusRewrite implements RewriteTask.RewriteQuery {

    @Autowired
    private VirtualCorpusService vcService;

    @Override
    public JsonNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) throws KustvaktException {
        if (node.has("collection")) {
            findVCRef(user.getUsername(), node.at("/collection"));
        }
        return node.rawNode();
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
                if (vcName.contains("/")) {
                    String[] names = vcName.split("/");
                    if (names.length == 2) {
                        vcOwner = names[0];
                        vcName = names[1];
                    }
                }

                VirtualCorpus vc =
                        vcService.searchVCByName(username, vcName, vcOwner);
                if (!vc.isCached()) {
                    rewriteVC(vc, koralNode);
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

    private void rewriteVC (VirtualCorpus vc, KoralNode koralNode)
            throws KustvaktException {
        String koralQuery = vc.getKoralQuery();
        JsonNode kq = JsonUtils.readTree(koralQuery).at("/collection");
        JsonNode jsonNode = koralNode.rawNode();
        // rewrite
        koralNode.remove("@type",
                new RewriteIdentifier("@type", jsonNode.at("/@type").asText()));
        koralNode.remove("ref",
                new RewriteIdentifier("ref", jsonNode.at("/ref").asText()));
        koralNode.setAll((ObjectNode) kq);
    }
}
