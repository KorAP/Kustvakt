package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author hanl
 * @date 30/06/2015
 */
// todo: do post processing!
//todo: load rewritenode and rewritequery automatically from classpath by default, but namespaced from package
public class RewriteHandler {

    private static Logger jlog = KustvaktLogger.getLogger(RewriteHandler.class);
    private Collection<RewriteTask.IterableRewriteAt> node_processors;
    private Collection<RewriteTask.RewriteKoralToken> token_node_processors;
    private Collection<RewriteTask.RewriteNodeAt> query_processors;

    //    private Collection<RewriteTask.RewriteNode2> fixed_nodes;
    //    private Collection<RewriteTask.IterableRewrite> iterable_nodes;

    private Set<Class> failed_task_registration;

    private KustvaktConfiguration config;

    // fixme: make default constructor with configuration!
    public RewriteHandler(KustvaktConfiguration config) {
        this.config = config;
        this.node_processors = new HashSet<>();
        this.token_node_processors = new HashSet<>();
        this.query_processors = new HashSet<>();
        this.failed_task_registration = new HashSet<>();
    }

    public boolean addProcessor(RewriteTask rewriter) {
        if (rewriter instanceof RewriteTask.RewriteKoralToken)
            return this.token_node_processors
                    .add((RewriteTask.RewriteKoralToken) rewriter);
        else if (rewriter instanceof RewriteTask.RewriteNodeAt)
            return this.query_processors
                    .add((RewriteTask.RewriteNodeAt) rewriter);
        else if (rewriter instanceof RewriteTask.IterableRewriteAt)
            return this.node_processors
                    .add((RewriteTask.IterableRewriteAt) rewriter);

        this.failed_task_registration.add(rewriter.getClass());
        return false;
    }

    public boolean addProcessor2(RewriteTask rewriteTask) {
        //        if (rewriteTask instanceof RewriteTask.RewriteNode2)
        //            return this.fixed_nodes.add((RewriteTask.RewriteNode2) rewriteTask);
        //        else if (rewriteTask instanceof RewriteTask.IterableRewrite)
        //            return this.iterable_nodes
        //                    .add((RewriteTask.IterableRewrite) rewriteTask);
        //        else if (rewriteTask instanceof RewriteTask.RewriteBefore)
        // gets the entire pre processed query injected.
        return false;
    }

    public final Collection<Class> getFailedHandlers() {
        return this.failed_task_registration;
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
            this.failed_task_registration.add(rewriter);
            return false;
        }
        return addProcessor(task);
    }

    public void clear() {
        this.node_processors.clear();
        this.query_processors.clear();
        this.token_node_processors.clear();
    }

    private boolean process(String name, JsonNode root, User user,
            boolean post) {
        if (root.isObject()) {
            if (root.has("operands")) {
                JsonNode ops = root.at("/operands");
                Iterator<JsonNode> it = ops.elements();
                while (it.hasNext()) {
                    JsonNode next = it.next();
                    if (process(name, next, user, post))
                        it.remove();
                }
            }else if (root.path("@type").asText().equals("koral:token")) {
                // todo: koral:token nodes cannot be flagged for deletion --> creates the possibility for empty koral:token nodes
                //                processIterableNode(KoralNode.wrapNode(root), user,
                //                        this.token_node_processors, post);
                return process(name, root.path("wrap"), user, post);
            }else {
                return processNode(name, KoralNode.wrapNode(root), user,
                        this.node_processors, post);
            }
        }else if (root.isArray()) {
            Iterator<JsonNode> it = root.elements();
            while (it.hasNext()) {
                JsonNode next = it.next();
                if (process(name, next, user, post))
                    it.remove();
            }
        }
        return false;
    }

    public JsonNode preProcess(JsonNode root, User user) {
        boolean post = false;
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> next = it.next();
            process(next.getKey(), next.getValue(), user, post);
        }
        processFixedNode(root, user, this.query_processors, post);
        return root;
    }

    public String preProcess(String json, User user) {
        return JsonUtils.toJSON(preProcess(JsonUtils.readTree(json), user));
    }

    public JsonNode postProcess(JsonNode root, User user) {
        boolean post = true;
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> next = it.next();
            process(next.getKey(), next.getValue(), user, post);
        }
        processFixedNode(root, user, this.query_processors, post);
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
    private boolean processNode(String rootNode, KoralNode node, User user,
            Collection<? extends RewriteTask.IterableRewriteAt> tasks,
            boolean post) {
        for (RewriteTask task : tasks) {
            jlog.debug("running processor on node: " + node);
            jlog.debug("on processor: " + task.getClass().toString());

            if (task instanceof RewriteTask.IterableRewriteAt) {
                RewriteTask.IterableRewriteAt rw = (RewriteTask.IterableRewriteAt) task;
                if (rw.path() != null && !rw.path().equals(rootNode)) {
                    jlog.debug("skipping node: " + node);
                    continue;
                }
            }

            if (!post && task instanceof RewriteTask.RewriteBefore)
                ((RewriteTask.RewriteBefore) task)
                        .preProcess(node, this.config, user);
            else if (task instanceof RewriteTask.RewriteAfter)
                ((RewriteTask.RewriteAfter) task).postProcess(node);

            if (node.isRemove())
                break;
        }
        return node.isRemove();
    }

    private void processFixedNode(JsonNode node, User user,
            Collection<RewriteTask.RewriteNodeAt> tasks, boolean post) {
        for (RewriteTask.RewriteNodeAt task : tasks) {
            if (!node.at(task.at()).isMissingNode()) {
                if (!post)
                    task.preProcess(KoralNode.wrapNode(node.at(task.at())),
                            this.config, user);
                else
                    task.postProcess(KoralNode.wrapNode(node.at(task.at())));
            }
        }
    }

}
