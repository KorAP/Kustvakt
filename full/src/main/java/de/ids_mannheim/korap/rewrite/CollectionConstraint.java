package de.ids_mannheim.korap.rewrite;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;

/** EM: not used anymore. This rewrite was meant to remove doc from 
 * a collection by checking user access to the doc.  
 * 
 * @author hanl
 * @date 03/07/2015
 */
public class CollectionConstraint implements RewriteTask.IterableRewritePath {

    @Override
    public KoralNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) {
        if (node.get("@type").equals("koral:doc")) {
            if (node.get("key").equals(Attributes.CORPUS_SIGLE)) {
                String id = node.get("value");
                // EM: MH checks if user has access to corpus
//                KustvaktResource corpus = check(id, user);
//                if (corpus == null)
                    node.removeNode(new KoralNode.RewriteIdentifier(
                            Attributes.CORPUS_SIGLE, id));
            }
        }
        return node;
    }


    /**
     * @param id
     * @param user
     * @return boolean if true access granted
     */
//    @Deprecated
//    private KustvaktResource check (String id, User user) {
//        // todo: can be used to circumvent access control if public filter not applied
//        if (user == null)
//            return null;
//
//        KustvaktResource corpus;
//        try {
//            SecurityManager m = SecurityManager
//                    .findbyId(id, user, Corpus.class);
//            corpus = m.getResource();
//        }
//        catch (RuntimeException | KustvaktException e) {
//            return null;
//        }
//        return corpus;
//    }


    @Override
    public JsonNode rewriteResult (KoralNode node) {
        return null;
    }


    @Override
    public String path () {
        return "collection";
    }
}
