package de.ids_mannheim.korap.resource.rewrite;

/**
 * query rewrites get injected the entire query from root containing all child nodes
 * <p/>
 * {@link de.ids_mannheim.korap.resource.rewrite.RewriteQuery} does not allow the deletion of the root node or subnode through KoralNode.
 * The {@link de.ids_mannheim.korap.resource.rewrite.RewriteHandler} will igonore respecitve invalid requests
 *
 * @author hanl
 * @date 03/07/2015
 */
public abstract class RewriteQuery extends RewriteTask {

    public RewriteQuery() {
        super();
    }

}
