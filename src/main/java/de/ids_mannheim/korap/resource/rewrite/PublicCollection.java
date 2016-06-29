package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.utils.JsonUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author hanl
 * @date 04/07/2015
 */
public class PublicCollection implements RewriteTask.RewriteQuery {

    public PublicCollection () {
        super();
    }


    @Override
    public JsonNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) throws KustvaktException {
        JsonNode subnode = node.rawNode();

        if (!subnode.at("/collection").findValuesAsText("key")
                .contains(Attributes.CORPUS_SIGLE)) {
            KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
            if (subnode.has("collection"))
                b.setBaseQuery(JsonUtils.toJSON(subnode));

            Set resources = ResourceFinder.search(user, Corpus.class);
            ArrayList<KustvaktResource> list = new ArrayList(resources);

            // fixme: throw exception in resourcefinder to indicate if no resource or no permission!
            if (list.isEmpty())
                throw new KustvaktException(
                        StatusCodes.NO_POLICY_PERMISSION,
                        "Resources could not be loaded for user ",
                        user.getUsername());

            Set ids = new HashSet(resources.size());
            for (int i = 0; i < list.size(); i++) {
                if (i > 0)
                    b.or();
                b.with(Attributes.CORPUS_SIGLE + "="
                        + list.get(i).getPersistentID());
                ids.add(list.get(i).getPersistentID());
            }
            JsonNode rewritten = JsonUtils.readTree(b.toJSON());
            node.set("collection", rewritten.at("/collection"),
                    new KoralNode.RewriteIdentifier(Attributes.CORPUS_SIGLE,
                            ids));
            node.at("/collection");
        }

        return node.rawNode();
    }
}
