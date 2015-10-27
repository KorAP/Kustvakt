package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;
import lombok.Getter;

/**
 * @author hanl
 * @date 30/06/2015
 */
@Getter
public abstract class RewriteTask {

    private RewriteTask() {
    }

    /**
     * @param node   Json node in KoralNode wrapper
     * @param config {@link KustvaktConfiguration} singleton instance to use default configuration parameters
     * @param user   injected by rewrite handler if available. Might cause {@link NullPointerException} if not checked properly
     * @return
     */
    public abstract JsonNode rewrite(KoralNode node,
            KustvaktConfiguration config, User user);

    /**
     * query rewrites get injected the entire query from root containing all child nodes
     * <p/>
     * {@link de.ids_mannheim.korap.resource.rewrite.RewriteTask.RewriteQuery} does not allow the deletion of the root node or subnode through KoralNode.
     * The {@link de.ids_mannheim.korap.resource.rewrite.RewriteHandler} will igonore respecitve invalid requests
     *
     * @author hanl
     * @date 03/07/2015
     */
    public static abstract class RewriteQuery extends RewriteTask {
        public RewriteQuery() {
            super();
        }
    }

    /**
     * node rewrites get injected typically object nodes that are subject to altering.
     * Be aware that node rewrites are processed before query rewrites. Thus query rewrite may override previous node rewrites
     * <p/>
     * {@link de.ids_mannheim.korap.resource.rewrite.RewriteTask.RewriteNode} rewrite support the deletion of the respective node by simply setting the node invalid in KoralNode
     *
     * @author hanl
     * @date 03/07/2015
     */
    public static abstract class RewriteNode extends RewriteTask {
        public RewriteNode() {
            super();
        }
    }
}
