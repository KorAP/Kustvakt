package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 30/06/2015
 */
public interface RewriteTask {


    /**
     * unspecified query rewrite that gets injected the entire root node during preprocessing
     */
    interface RewriteBefore extends RewriteTask {
        /**
         * @param node
         *            Json node in KoralNode wrapper
         * @param config
         *            {@link KustvaktConfiguration} singleton instance
         *            to use default configuration parameters
         * @param user
         *            injected by rewrite handler if available. Might
         *            cause {@link NullPointerException} if not
         *            checked properly
         * @return
         */
        JsonNode preProcess (KoralNode node, KustvaktConfiguration config,
                User user) throws KustvaktException;

    }

    /**
     * Post processor targeted at result sets for queries
     * {@link de.ids_mannheim.korap.resource.rewrite.RewriteTask.RewriteAfter}
     * queries will run
     * after
     * {@link de.ids_mannheim.korap.resource.rewrite.RewriteTask.IterableRewriteAt}
     * have been processed
     */
    interface RewriteAfter extends RewriteTask {
        JsonNode postProcess (KoralNode node) throws KustvaktException;
    }

    /**
     * nodes subject to rewrites at fixed json pointer location.
     * Json-pointer based rewrites are processed after iterable
     * rewrites
     * Deletion via KoralNode not allowed. Supports pre- and
     * post-processing
     */
    interface RewriteNodeAt extends RewriteBefore, RewriteAfter {
        String at ();
    }

    /**
     * terminal object nodes that are subject to rewrites through node
     * iteration
     * (both object and array node iteration supported)
     */
    interface IterableRewriteAt extends RewriteBefore, RewriteAfter {
        String path ();
    }

    /**
     * koral token nodes that are subject to rewrites
     * Be aware that node rewrites are processed before query
     * rewrites. Thus query rewrite may override previous node
     * rewrites {@link RewriteKoralToken} rewrite DOES NOT support the
     * deletion of the respective node
     */
    interface RewriteKoralToken extends RewriteBefore {}

}
