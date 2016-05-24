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
import java.util.Set;

/**
 * @author hanl
 * @date 04/07/2015
 */
public class PublicCollection implements RewriteTask.RewriteBefore {

    public PublicCollection () {
        super();
    }


    // todo: where to inject the array node into? --> super group with and relation plus subgroup with ids and or operator
    @Override
    public JsonNode preProcess (KoralNode node, KustvaktConfiguration config,
            User user) {
        JsonNode subnode = node.rawNode();

        if (!subnode.at("/collection").findValuesAsText("key")
                .contains(Attributes.CORPUS_SIGLE)) {
            KoralCollectionQueryBuilder b = new KoralCollectionQueryBuilder();
            if (subnode.has("collection"))
                b.setBaseQuery(JsonUtils.toJSON(subnode));

            try {
                Set resources = ResourceFinder.search(user, Corpus.class);
                ArrayList<KustvaktResource> list = new ArrayList(resources);

                if (list.isEmpty())
                    throw new KustvaktException(StatusCodes.REQUEST_INVALID,
                            "No resources found for user", user.getUsername());

                for (int i = 0; i < list.size(); i++) {
                    if (i > 0)
                        b.or();
                    b.with(Attributes.CORPUS_SIGLE+"=" + list.get(i).getPersistentID());
                }
                JsonNode rewritten = JsonUtils.readTree(b.toJSON());
                node.set("collection", rewritten.at("/collection"));
            }
            catch (KustvaktException e) {
                e.printStackTrace();
                // todo:
            }
        }

        return node.rawNode();
    }
}
