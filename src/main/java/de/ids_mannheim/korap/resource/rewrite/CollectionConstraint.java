package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.security.ac.SecurityManager;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 03/07/2015
 */
// todo: test
public class CollectionConstraint extends RewriteTask.RewriteNode {

    @Override
    public JsonNode rewrite(KoralNode koralnode, KustvaktConfiguration config,
            User user) {
        JsonNode node = koralnode.rawNode();
        if (node.at("/@type").asText().equals("koral:doc")) {
            if (node.at("/key").asText().equals("corpusID") && !check(koralnode,
                    user)) {
                koralnode.removeNode();
                // todo: add message that node was removed!
            }
        }
        return node;
    }

    private boolean check(KoralNode node, User user) {
        if (user == null)
            return true;

        String id = node.rawNode().at("/value").asText();
        KustvaktResource corpus;
        try {
            SecurityManager m = SecurityManager
                    .findbyId(id, user, Corpus.class);
            corpus = m.getResource();
        }catch (KustvaktException e) {
            return false;
        }
        return corpus != null;
    }

}
