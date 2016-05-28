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
     * unspecified query rewrite that gets injected the entire root
     * node during preprocessing
     */
    interface RewriteQuery extends RewriteTask {
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
        JsonNode rewriteQuery (KoralNode node, KustvaktConfiguration config,
                User user) throws KustvaktException;

    }

    /**
     * Post processor targeted at result sets for queries
     * {@link RewriteResult} queries will run
     * after {@link IterableRewritePath} have been processed
     */
    interface RewriteResult extends RewriteTask {
        JsonNode rewriteResult (KoralNode node) throws KustvaktException;
    }

    /**
     * nodes subject to rewrites at fixed json pointer location.
     * Json-pointer based rewrites are processed after iterable
     * rewrites
     * Deletion via KoralNode not allowed. Supports pre- and
     * post-processing
     */
    interface RewriteNodeAt extends RewriteQuery, RewriteResult {
        String at ();
    }

    /**
     * terminal object nodes that are subject to rewrites through node
     * iteration
     * (both object and array node iteration supported)
     */
    interface IterableRewritePath extends RewriteQuery, RewriteResult {
        String path ();
    }

    /**
     * koral token nodes that are subject to rewrites
     * Be aware that node rewrites are processed before query
     * rewrites. Thus query rewrite may override previous node
     * rewrites {@link RewriteKoralToken} rewrite DOES NOT support the
     * deletion of the respective node
     */
    interface RewriteKoralToken extends RewriteQuery {}

}
