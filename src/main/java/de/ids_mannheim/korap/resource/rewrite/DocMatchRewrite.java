package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.*;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.resources.Document;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 12/11/2015
 */
//todo : test
public class DocMatchRewrite extends KustvaktCacheable implements RewriteTask.IterableRewritePath,
        BeanInjectable {

    private ResourceOperationIface<Document> docDao;

    public DocMatchRewrite () {
        super("documents", "key:doc");
    }


    @Override
    public void insertBeans (ContextHolder beans) {
        this.docDao = BeansFactory.getTypeFactory().getTypeInterfaceBean(
                beans.getResourceProviders(), Document.class);
    }


    //todo: benchmark: see if retrieval and and get docs for all ids at once is better --> outside this rewrite handler
    @Override
    public JsonNode rewriteResult (KoralNode node) throws KustvaktException {
        Document doc;
        if (this.docDao == null)
            throw new RuntimeException("Document dao must be set!");

        if (node.has(Attributes.TEXT_SIGLE)) {
            String textSigle = node.get(Attributes.TEXT_SIGLE);
            Object o = this.getCacheValue(textSigle);
            if (o == null) {
                doc = docDao.findbyId(textSigle, null);
                if (doc != null)
                    this.storeInCache(textSigle, doc);
            }
            else
                doc = (Document) o;

            if (doc != null && doc.isDisabled())
                node.removeNode(new KoralNode.RewriteIdentifier(
                        Attributes.TEXT_SIGLE, doc.getPersistentID()));
        }
        return node.rawNode();
    }


    @Override
    public String path () {
        return "matches";
    }


    @Override
    public JsonNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
            User user) {
        return null;
    }
}
