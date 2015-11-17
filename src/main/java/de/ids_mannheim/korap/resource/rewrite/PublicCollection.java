package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.CollectionQueryBuilder3;

import java.util.Set;

/**
 * @author hanl
 * @date 04/07/2015
 */
// todo: 11.11.15
public class PublicCollection implements RewriteTask.RewriteNodeAt {

    public PublicCollection() {
        super();
    }

    // todo: where to inject the array node into? --> super group with and relation plus subgroup with ids and or operation
    @Override
    public JsonNode preProcess(KoralNode node, KustvaktConfiguration config,
            User user) {
        JsonNode subnode = node.rawNode();
        // todo: test
        if (!subnode.at("/collection").findValuesAsText("key")
                .contains("corpusID")) {
            //todo: inject public collection node
            if (user != null) {
                try {
                    ResourceFinder finder = ResourceFinder
                            .init(user, Corpus.class);
                    Set<String> ids = finder.getIds();
                    createNode(ids);
                }catch (KustvaktException e) {
                    e.printStackTrace();
                    //todo: 20.10.15 - 21.10.15
                }
            }
        }
        return subnode;
    }

    //todo: 20.10.15
    private JsonNode createNode(Set<String> ids) {
        JsonNode node = CollectionQueryBuilder3.Utils.buildDocGroup();
        return node;
    }

    @Override
    public JsonNode postProcess(KoralNode node) {
        return null;
    }

    @Override
    public String at() {
        return "collection";
    }
}
