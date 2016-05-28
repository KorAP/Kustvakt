package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.Attributes;
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
public class CollectionConstraint implements RewriteTask.IterableRewritePath {



    @Override
    public JsonNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) {
        if (node.get("@type").equals("koral:doc")) {
            if (node.get("key").equals(Attributes.CORPUS_SIGLE)) {
                String id = node.get("value");
                KustvaktResource corpus = check(id, user);
                if (corpus == null)
                    node.removeNode(new KoralNode.RewriteIdentifier(
                            Attributes.CORPUS_SIGLE, id));
            }
        }
        return node.rawNode();
    }


    /**
     * @param id
     * @param user
     * @return boolean if true access granted
     */
    private KustvaktResource check (String id, User user) {
        // todo: can be used to circumvent access control if public filter not applied
        if (user == null)
            return null;

        KustvaktResource corpus;
        try {
            SecurityManager m = SecurityManager
                    .findbyId(id, user, Corpus.class);
            corpus = m.getResource();
        }
        catch (RuntimeException | KustvaktException e) {
            return null;
        }
        return corpus;
    }


    @Override
    public JsonNode rewriteResult (KoralNode node) {
        return null;
    }


    @Override
    public String path () {
        return "collection";
    }
}
