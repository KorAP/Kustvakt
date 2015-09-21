package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.security.ac.SecurityManager;

/**
 * @author hanl
 * @date 03/07/2015
 */
// todo: test
public class CollectionConstraint extends RewriteTask.RewriteNode {

    public CollectionConstraint() {
        super();
    }

    @Override
    public JsonNode rewrite(KoralNode koralnode) {
        JsonNode node = koralnode.rawNode();
        if (node.at("/@type").asText().equals("koral:doc")) {
            if (node.at("/key").asText().equals("corpusID") && !check(
                    koralnode)) {
                koralnode.removeNode();
                // todo: add message that node was removed!
            }
        }
        return node;
    }

    private boolean check(KoralNode node) {
        if (!node.hasUser())
            return true;

        String id = node.rawNode().at("/value").asText();
        KustvaktResource corpus;
        try {
            SecurityManager m = SecurityManager
                    .findbyId(id, node.getUser(), Corpus.class);
            corpus = m.getResource();
        }catch (KustvaktException e) {
            return false;
        }
        return corpus != null;
    }

}
