package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import de.ids_mannheim.korap.utils.CollectionQueryBuilder3;

import java.util.Set;

/**
 * @author hanl
 * @date 04/07/2015
 */
public class PublicCollection extends RewriteTask.RewriteQuery {

    // todo: where to inject the array node into? --> super group with and relation plus subgroup with ids and or operation
    @Override
    public JsonNode rewrite(KoralNode node) {
        JsonNode subnode = node.rawNode();
        if (!subnode.at("/collection").findValuesAsText("key")
                .contains("corpusID")) {
            //todo: inject public collection node
            if (node.hasUser()) {
                try {
                    ResourceFinder finder = ResourceFinder
                            .init(node.getUser(), Corpus.class);
                    Set<String> ids = finder.getIds();
                    createNode(ids);
                }catch (KustvaktException e) {
                    e.printStackTrace();
                }
            }
        }
        return subnode;
    }

    private JsonNode createNode(Set<String> ids) {
        JsonNode node = CollectionQueryBuilder3.Utils.buildDocGroup();
        return node;
    }
}
