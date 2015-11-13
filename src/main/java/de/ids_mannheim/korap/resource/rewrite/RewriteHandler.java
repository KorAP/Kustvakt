package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author hanl
 * @date 30/06/2015
 */
// todo: do post processing!
//todo: load rewritenode and rewritequery automatically from classpath by default, but namespaced from package
public class RewriteHandler {

    private static Logger jlog = KustvaktLogger.getLogger(RewriteHandler.class);
    private Collection<RewriteTask> node_processors;
    private Collection<RewriteTask.RewriteKoralToken> token_node_processors;
    private Collection<RewriteTask.RewriteQuery> query_processors;

    private KustvaktConfiguration config;

    // fixme: make default constructor with configuration!
    public RewriteHandler(KustvaktConfiguration config) {
        this.config = config;
        this.node_processors = new HashSet<>();
        this.token_node_processors = new HashSet<>();
        this.query_processors = new HashSet<>();
    }

    public boolean addProcessor(RewriteTask rewriter) {
        if (rewriter instanceof RewriteTask.RewriteKoralToken)
            return this.token_node_processors
                    .add((RewriteTask.RewriteKoralToken) rewriter);
        else if (rewriter instanceof RewriteTask.RewriteQuery)
            return this.query_processors
                    .add((RewriteTask.RewriteQuery) rewriter);
        else if (rewriter instanceof RewriteTask.RewriteBefore
                | rewriter instanceof RewriteTask.RewriteAfter)
            return this.node_processors.add(rewriter);
        //        else if (rewriter instanceof RewriteTask.RewriteAfter)
        //            return this.node_post_processors
        //                    .add((RewriteTask.RewriteAfter) rewriter);
        return false;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("--------------------------");
        b.append("pre/post: " + this.node_processors.toString()).append("\n")
                .append("\n")
                .append("query: " + this.query_processors.toString())
                .append("\n")
                .append("koraltoken: " + this.token_node_processors.toString());
        b.append("---------------------------");
        return b.toString();
    }

    /**
     * expects extended RewriteNode/Query class with empty default constructor
     *
     * @param rewriter
     * @return boolean if rewriter class was successfully added to rewrite handler!
     */
    public boolean add(Class<? extends RewriteTask> rewriter) {
        RewriteTask task;
        try {
            Constructor c = rewriter.getConstructor();
            task = (RewriteTask) c.newInstance();
        }catch (NoSuchMethodException | InvocationTargetException
                | IllegalAccessException | InstantiationException e) {
            return false;
        }
        return addProcessor(task);
    }

    public void clear() {
        this.node_processors.clear();
        this.query_processors.clear();
        this.token_node_processors.clear();
    }

    private boolean process(JsonNode root, User user, boolean post) {
        if (root.isObject()) {
            if (root.has("operands")) {
                JsonNode ops = root.at("/operands");
                Iterator<JsonNode> it = ops.elements();
                while (it.hasNext()) {
                    JsonNode n = it.next();
                    if (process(n, user, post)) {
                        it.remove();
                    }
                }

            }else if (root.path("@type").asText().equals("koral:token")) {
                // todo: koral:token nodes cannot be flagged for deletion --> creates the possibility for empty koral:token nodes
                processNode(KoralNode.wrapNode(root), user,
                        this.token_node_processors, post);
                return process(root.path("wrap"), user, post);
            }else {
                return processNode(KoralNode.wrapNode(root), user,
                        this.node_processors, post);
            }
        }else if (root.isArray()) {
            //todo: test!
            Iterator<JsonNode> it = root.elements();
            while (it.hasNext()) {
                JsonNode n = it.next();
                if (process(n, user, post)) {
                    it.remove();
                }
            }
        }
        return false;
    }

    public JsonNode preProcess(JsonNode root, User user) {
        boolean post = false;
        for (JsonNode n : root)
            process(n, user, post);
        processNode(KoralNode.wrapNode(root), user, this.query_processors,
                post);
        return root;
    }

    public String preProcess(String json, User user) {
        return JsonUtils.toJSON(preProcess(JsonUtils.readTree(json), user));
    }

    public JsonNode postProcess(JsonNode root, User user) {
        boolean post = true;
        for (JsonNode n : root)
            process(n, user, post);
        processNode(KoralNode.wrapNode(root), user, this.query_processors,
                post);
        return root;
    }

    public String postProcess(String json, User user) {
        return JsonUtils.toJSON(postProcess(JsonUtils.readTree(json), user));
    }

    /**
     * @param node
     * @param user
     * @param tasks
     * @return boolean true if node is to be removed from parent! Only applies if parent is an array node
     */
    // todo: integrate notifications into system!
    private boolean processNode(KoralNode node, User user,
            Collection<? extends RewriteTask> tasks, boolean post) {
        for (RewriteTask task : tasks) {
            if (jlog.isDebugEnabled()) {
                jlog.debug("running processor on node " + node);
                jlog.debug("on processor " + task.getClass().toString());
            }
            if (task instanceof RewriteTask.RewriteBefore)
                ((RewriteTask.RewriteBefore) task)
                        .preProcess(node, this.config, user);
            if (post && task instanceof RewriteTask.RewriteAfter)
                ((RewriteTask.RewriteAfter) task).postProcess(node);
            if (node.toRemove())
                break;
        }
        return node.toRemove();
    }

}
