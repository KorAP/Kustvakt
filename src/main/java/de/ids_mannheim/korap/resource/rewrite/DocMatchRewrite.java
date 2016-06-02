package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.*;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.DocumentDao;
import de.ids_mannheim.korap.resources.Document;
import de.ids_mannheim.korap.user.User;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.w3c.dom.Attr;

/**
 * @author hanl
 * @date 12/11/2015
 */
//todo : test
public class DocMatchRewrite implements RewriteTask.IterableRewritePath,
        BeanInjectable {

    private DocumentDao docDao;
    private Cache cache;


    public DocMatchRewrite () {
        this.cache = CacheManager.getInstance().getCache("documents");
    }


    @Override
    public void insertBeans (ContextHolder beans) {
        this.docDao = BeansFactory.getTypeFactory().getTypedBean(
                beans.getResourceProvider(), Document.class);
    }


    //todo: benchmark: see if retrieval and and get docs for all ids at once is better --> outside this rewrite handler
    @Override
    public JsonNode rewriteResult (KoralNode node) throws KustvaktException {
        Document doc;
        if (this.docDao == null)
            throw new RuntimeException("Document dao must be set!");

        if (node.has(Attributes.TEXT_SIGLE)) {
            String textSigle = node.get(Attributes.TEXT_SIGLE);
            Element e = this.cache.get(textSigle);
            if (e == null) {
                doc = docDao.findbyId(textSigle, null);
                if (doc != null)
                    this.cache.put(new Element(textSigle, doc));
            }
            else
                doc = (Document) e.getObjectValue();

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
