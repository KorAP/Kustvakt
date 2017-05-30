package de.ids_mannheim.korap.resource.rewrite;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;

/**
 * @author margaretha
 * @date 30 May 2017
 */
public class CollectionRewrite implements RewriteTask.RewriteQuery {

    private static Logger jlog = LoggerFactory
            .getLogger(CollectionRewrite.class);
    public static String AVAILABILITY = "availability";

    public static Pattern notFreeLicense = Pattern.compile("ACA|QAO");
    public static Pattern notPublicLicense = Pattern.compile("QAO");


    public CollectionRewrite () {
        super();
    }


    private String verifyAvailability (JsonNode node, CorpusAccess access,
            KustvaktConfiguration config) {

        if (node.has("operands")) {
            ArrayList<JsonNode> operands = Lists
                    .newArrayList(node.at("/operands").elements());
            for (int i = 0; i < operands.size(); i++) {
                String path = verifyAvailability(operands.get(i), access,
                        config);
                if (!path.isEmpty()) { return "/operands/" + i; }
            }
        }
        else if (node.has("key")
                && node.at("/key").asText().equals(AVAILABILITY)) {
            Matcher m;
            String queryAvailability = node.at("/value").asText();
            if (node.at("/match").asText()
                    .equals(KoralMatchOperator.EQUALS.toString())) {

                if (access.equals(CorpusAccess.FREE)) {
                    m = notFreeLicense.matcher(queryAvailability);
                    if (m.find()) return "/value";
                }
                else if (access.equals(CorpusAccess.PUB)) {
                    m = notPublicLicense.matcher(queryAvailability);
                    if (m.find()) return "/value";
                }
            }
            // match:ne
            else {
                if (access.equals(CorpusAccess.FREE)) {
                    m = config.getFreeLicensePattern()
                            .matcher(queryAvailability);
                    if (m.find()) return "/value";
                }
                else if (access.equals(CorpusAccess.PUB)) {
                    m = config.getPublicLicensePattern()
                            .matcher(queryAvailability);
                    if (m.find()) return "/value";
                }
            }
        }

        return "";
    }


    @Override
    public JsonNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) throws KustvaktException {
        JsonNode jsonNode = node.rawNode();
        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
        switch (user.getCorpusAccess()) {
            case PUB:
                builder.with(
                        "availability = /CC-BY.*/ | availability = /ACA.*/");
                break;
            case ALL:
                builder.with("availability = /QAO.*/ | availability = /ACA.*/ |"
                        + "  availability = /CC-BY.*/");
                break;
            case FREE:
                builder.with("availability   = /CC-BY.*/");
                break;
        }

        RewriteIdentifier identifier = new KoralNode.RewriteIdentifier(
                Attributes.AVAILABILITY, user.getCorpusAccess());
        JsonNode rewrittesNode;

        if (jsonNode.has("collection")) {
            String path = verifyAvailability(jsonNode.at("/collection"),
                    user.getCorpusAccess(), config);
            if (!path.isEmpty()) {
                rewrittesNode = JsonUtils.readTree(builder.toJSON())
                        .at("/collection");
                if (path.equals("/value")) {
                    node.replace("collection", rewrittesNode, identifier);
                }
                else {
                    node.replaceAt("/collection" + path, rewrittesNode,
                            identifier);
                }
            }
            else {
                builder.setBaseQuery(builder.toJSON());
                rewrittesNode = builder.mergeWith(jsonNode).at("/collection");
                node.set("collection", rewrittesNode, identifier);
            }
        }
        else {
            rewrittesNode = JsonUtils.readTree(builder.toJSON())
                    .at("/collection");
            node.set("collection", rewrittesNode, identifier);
        }

        jlog.debug("REWRITES: " + node.at("/collection").toString());
        return node.rawNode();
    }
}
