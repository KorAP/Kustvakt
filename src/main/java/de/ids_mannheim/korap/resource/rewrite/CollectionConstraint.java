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
public class CollectionConstraint implements RewriteTask.RewriteNode {

    @Override
    public JsonNode preProcess(KoralNode node, KustvaktConfiguration config,
            User user) {

        if (node.get("@type").equals("koral:doc")) {
            if (node.get("key").equals("corpusID") && !check(node, user)) {
                node.removeNode();
                // todo: add message that node was removed!
            }
        }
        return node.rawNode();
    }

    /**
     * @param node
     * @param user
     * @return boolean if true access granted
     */
    private boolean check(KoralNode node, User user) {
        // todo: can be used to circumvent access control!
        if (user == null)
            return true;

        String id = node.get("value");
        KustvaktResource corpus;
        try {
            SecurityManager m = SecurityManager
                    .findbyId(id, user, Corpus.class);
            corpus = m.getResource();
        }catch (RuntimeException | KustvaktException e) {
            e.printStackTrace();
            return false;
        }

        return corpus != null;
    }

}
