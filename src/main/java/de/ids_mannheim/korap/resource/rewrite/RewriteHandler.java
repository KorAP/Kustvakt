package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanInjectable;
import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author hanl
 * @date 30/06/2015
 */
// todo: do post processing!
//todo: load rewritenode and rewritequery automatically from classpath by default, but namespaced from package
public class RewriteHandler implements BeanInjectable {

    private static Logger jlog = LoggerFactory.getLogger(RewriteHandler.class);
    private Collection<RewriteTask.IterableRewritePath> node_processors;
    private Collection<RewriteTask.RewriteKoralToken> token_node_processors;
    private Collection<RewriteTask> query_processors;

    private Set<Class> failed_task_registration;
    private KustvaktConfiguration config;
    private ContextHolder beans;


    public RewriteHandler (KustvaktConfiguration config) {
        this();
        this.config = config;
    }


    public RewriteHandler () {
        this.node_processors = new HashSet<>();
        this.token_node_processors = new HashSet<>();
        this.query_processors = new HashSet<>();
        this.failed_task_registration = new HashSet<>();
        this.beans = null;
    }


    public void defaultRewriteConstraints () {
        this.add(FoundryInject.class);
        this.add(PublicCollection.class);
        this.add(IdWriter.class);
        this.add(DocMatchRewrite.class);
        this.add(CollectionCleanupFilter.class);
    }


    public Set getFailedProcessors () {
        return this.failed_task_registration;
    }


    public boolean addProcessor (RewriteTask rewriter) {
        if (rewriter instanceof RewriteTask.RewriteKoralToken)
            return this.token_node_processors
                    .add((RewriteTask.RewriteKoralToken) rewriter);
        else if (rewriter instanceof RewriteTask.IterableRewritePath)
            return this.node_processors
                    .add((RewriteTask.IterableRewritePath) rewriter);
        else if (rewriter instanceof RewriteTask.RewriteBefore
                | rewriter instanceof RewriteTask.RewriteAfter)
            return this.query_processors.add(rewriter);

        this.failed_task_registration.add(rewriter.getClass());
        return false;
    }


    @Override
    public String toString () {
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
     * expects extended RewriteNode/Query class with empty default
     * constructor
     * 
     * @param rewriter
     * @return boolean if rewriter class was successfully added to
     *         rewrite handler!
     */
    public boolean add (Class<? extends RewriteTask> rewriter) {
        RewriteTask task;
        try {
            Constructor c = rewriter.getConstructor();
            task = (RewriteTask) c.newInstance();
        }
        catch (NoSuchMethodException | InvocationTargetException
                | IllegalAccessException | InstantiationException e) {
            this.failed_task_registration.add(rewriter);
            return false;
        }
        return addProcessor(task);
    }


    public void clear () {
        this.node_processors.clear();
        this.query_processors.clear();
        this.token_node_processors.clear();
    }


    private boolean processNode (String name, JsonNode root, User user, boolean post) {
        if (root.isObject()) {
            if (root.has("operands")) {
                JsonNode ops = root.at("/operands");
                Iterator<JsonNode> it = ops.elements();
                while (it.hasNext()) {
                    JsonNode next = it.next();
                    if (processNode(name, next, user, post))
                        it.remove();
                }
            }
            else if (root.path("@type").asText().equals("koral:token")) {
                // todo: koral:token nodes cannot be flagged for deletion --> creates the possibility for empty koral:token nodes
                rewrite(name, KoralNode.wrapNode(root), user,
                        this.token_node_processors, post);
                return processNode(name, root.path("wrap"), user, post);
            }
            else {
                return rewrite(name, KoralNode.wrapNode(root), user,
                        this.node_processors, post);
            }
        }
        else if (root.isArray()) {
            Iterator<JsonNode> it = root.elements();
            while (it.hasNext()) {
                JsonNode next = it.next();
                if (processNode(name, next, user, post))
                    it.remove();
            }
        }
        return false;
    }


    private JsonNode iterate (JsonNode root, User user, boolean post) {
        jlog.debug("Running rewrite process on query {}", root);
        if (root != null) {
            Iterator<Map.Entry<String, JsonNode>> it = root.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> next = it.next();
                processNode(next.getKey(), next.getValue(), user, post);
            }
            processFixedNode(root, user, this.query_processors, post);
        }
        return root;
    }


    public JsonNode process(JsonNode root, User user) {
        JsonNode pre = iterate(root, user, false);
        return iterate(pre, user, true);
    }


    public String process(String json, User user) {
        return JsonUtils.toJSON(process(JsonUtils.readTree(json), user));
    }


    /**
     * @param node
     * @param user
     * @param tasks
     * @return boolean true if node is to be removed from parent! Only
     *         applies if parent is an array node
     */
    private boolean rewrite (String rootNode, KoralNode node, User user,
            Collection<? extends RewriteTask> tasks, boolean post) {
        if (this.config == null)
            throw new RuntimeException("KustvaktConfiguration must be set!");

        for (RewriteTask task : tasks) {
            jlog.debug("running processor on node: " + node);
            jlog.debug("on processor: " + task.getClass().toString());

            if (this.beans != null && task instanceof BeanInjectable)
                ((BeanInjectable) task).insertBeans(this.beans);

            if (task instanceof RewriteTask.IterableRewritePath) {
                RewriteTask.IterableRewritePath rw = (RewriteTask.IterableRewritePath) task;
                if (rw.path() != null && !rw.path().equals(rootNode)) {
                    jlog.debug("skipping node: " + node);
                    continue;
                }
            }
            try {
                if (!post && task instanceof RewriteTask.RewriteBefore) {
                    ((RewriteTask.RewriteBefore) task).preProcess(node,
                            this.config, user);
                }
                else if (task instanceof RewriteTask.RewriteAfter) {
                    ((RewriteTask.RewriteAfter) task).postProcess(node);
                }
                node.buildRewrites();
            }
            catch (KustvaktException e) {
                jlog.error("Error in rewrite processor {} for node {}", task
                        .getClass().getSimpleName(), node.rawNode().toString());
                e.printStackTrace();
            }
            if (node.isRemove())
                break;
        }
        return node.isRemove();
    }


    private void processFixedNode (JsonNode node, User user,
            Collection<RewriteTask> tasks, boolean post) {
        for (RewriteTask task : tasks) {
            KoralNode next = KoralNode.wrapNode(node);
            if (task instanceof RewriteTask.RewriteNodeAt) {
                RewriteTask.RewriteNodeAt rwa = (RewriteTask.RewriteNodeAt) task;
                if ((rwa.at() != null && !node.at(rwa.at()).isMissingNode()))
                    next = next.at(rwa.at());
            }

            try {
                if (!post & task instanceof RewriteTask.RewriteBefore)
                    ((RewriteTask.RewriteBefore) task).preProcess(next,
                            this.config, user);
                else if (task instanceof RewriteTask.RewriteAfter)
                    ((RewriteTask.RewriteAfter) task).postProcess(next);
                next.buildRewrites();
            }
            catch (KustvaktException e) {
                jlog.error("Error in rewrite processor {} for node {}", task
                        .getClass().getSimpleName(), next.toString());
                e.printStackTrace();
            }
        }
    }


    @Override
    public <T extends ContextHolder> void insertBeans (T beans) {
        this.beans = beans;
        this.config = beans.getConfiguration();
    }
}
