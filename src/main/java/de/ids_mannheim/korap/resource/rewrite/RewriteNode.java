package de.ids_mannheim.korap.resource.rewrite;

/**
 * node rewrites get injected typically object nodes that are subject to altering.
 * Be aware that node rewrites are processed before query rewrites. Thus query rewrite may override previous node rewrites
 *
 * {@link de.ids_mannheim.korap.resource.rewrite.RewriteNode} rewrite support the deletion of the respective node by simply setting the node invalid in KoralNode
 *
 * @author hanl
 * @date 03/07/2015
 */
public abstract class RewriteNode extends RewriteTask {

    public RewriteNode() {
        super();
    }



}
