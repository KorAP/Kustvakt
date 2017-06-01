package de.ids_mannheim.korap.resource.rewrite;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resource.rewrite.KoralNode.RewriteIdentifier;
import de.ids_mannheim.korap.user.User;
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


    public CollectionRewrite () {
        super();
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
            builder.setBaseQuery(builder.toJSON());
            rewrittesNode = builder.mergeWith(jsonNode).at("/collection");
            node.set("collection", rewrittesNode, identifier);
        }
        else {
            rewrittesNode = JsonUtils.readTree(builder.toJSON())
                    .at("/collection");
            node.set("collection", rewrittesNode, identifier);
        }

        jlog.info("REWRITES: " + node.at("/collection").toString());
        return node.rawNode();
    }
}
