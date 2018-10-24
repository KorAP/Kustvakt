package de.ids_mannheim.korap.resource.rewrite;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.BeanInjectable;
import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl
 * @date 30/06/2015
 */
// todo: do post processing!
//todo: load rewritenode and rewritequery automatically from classpath by default, but namespaced from package
public class RewriteHandler{
    //implements BeanInjectable {

    private static Logger jlog = LogManager.getLogger(RewriteHandler.class);
    private Collection<RewriteTask.IterableRewritePath> node_processors;
    private Collection<RewriteTask.RewriteKoralToken> token_node_processors;
    private Collection<RewriteTask> query_processors;

    private Set<Class> failed_task_registration;
    @Autowired
    private KustvaktConfiguration config;
    private ContextHolder beans;


    public RewriteHandler (KustvaktConfiguration config) {
        this();
        this.config = config;
    }


    public RewriteHandler () {
        this.node_processors = new HashSet<>();
        this.token_node_processors = new HashSet<>();
        this.query_processors = new LinkedHashSet<>();
        this.failed_task_registration = new HashSet<>();
        this.beans = null;
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
        else if (rewriter instanceof RewriteTask.RewriteQuery
                | rewriter instanceof RewriteTask.RewriteResult)
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



    public String processQuery (JsonNode root, User user)
            throws KustvaktException {
        RewriteProcess process = new RewriteProcess(root, user);
        JsonNode pre = process.start(false);
        return JsonUtils.toJSON(pre);
    }


    public String processQuery (String json, User user)
            throws KustvaktException {
        return processQuery(JsonUtils.readTree(json), user);
    }


    public String processResult (String json, User user)
            throws KustvaktException {
        return processResult(JsonUtils.readTree(json), user);
    }


    public String processResult (JsonNode node, User user)
            throws KustvaktException {
        RewriteProcess process = new RewriteProcess(node, user);
        JsonNode pre = process.start(true);
        return JsonUtils.toJSON(pre);
    }


    public void clear () {
        this.node_processors.clear();
        this.query_processors.clear();
        this.token_node_processors.clear();
    }


    public <T extends ContextHolder> void insertBeans (T beans) {
        this.beans = beans;
        this.config = beans.getConfiguration();
    }



    public class RewriteProcess {

        private JsonNode root;
        private User user;


        private RewriteProcess (JsonNode root, User user) {
            this.root = root;
            this.user = user;
        }


        private KoralNode processNode (String key, JsonNode value,
                boolean result) throws KustvaktException {
            KoralNode kroot = KoralNode.wrapNode(value);
            if (value.isObject()) {
                if (value.has("operands")) {
                    JsonNode ops = value.at("/operands");
                    Iterator<JsonNode> it = ops.elements();
                    while (it.hasNext()) {
                        JsonNode next = it.next();
                        KoralNode kn = processNode(key, next, result);
                        if (kn.isRemove())
                            it.remove();
                    }
                }
                else if (value.path("@type").asText().equals("koral:token")) {
                    // todo: koral:token nodes cannot be flagged for deletion --> creates the possibility for empty koral:token nodes
                    rewrite(key, kroot,
                            RewriteHandler.this.token_node_processors, result);
                    return processNode(key, value.path("wrap"), result);
                }
                else {
                    return rewrite(key, kroot,
                            RewriteHandler.this.node_processors, result);
                }
            }
            else if (value.isArray()) {
                Iterator<JsonNode> it = value.elements();
                while (it.hasNext()) {
                    JsonNode next = it.next();
                    KoralNode kn = processNode(key, next, result);
                    if (kn.isRemove())
                        it.remove();
                }
            }
            return kroot;
        }


        private JsonNode start (boolean result) throws KustvaktException {
            jlog.debug("Running rewrite process on query "+ root);
            if (root != null) {
                Iterator<Map.Entry<String, JsonNode>> it = root.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> next = it.next();
                    processNode(next.getKey(), next.getValue(), result);
                }
                processFixedNode(root, RewriteHandler.this.query_processors,
                        result);
            }
            return root;
        }


        /**
         * @param node
         * @param tasks
         * @return boolean true if node is to be removed from parent!
         *         Only
         *         applies if parent is an array node
         */
        private KoralNode rewrite (String rootNode, KoralNode node,
                Collection<? extends RewriteTask> tasks, boolean result)
                throws KustvaktException {
            if (RewriteHandler.this.config == null)
                throw new RuntimeException("KustvaktConfiguration must be set!");

            for (RewriteTask task : tasks) {
                jlog.debug("running processor on node: " + node);
                jlog.debug("on processor: " + task.getClass().toString());

                if (RewriteHandler.this.beans != null
                        && task instanceof BeanInjectable)
                    ((BeanInjectable) task)
                            .insertBeans(RewriteHandler.this.beans);

                if (task instanceof RewriteTask.IterableRewritePath) {
                    RewriteTask.IterableRewritePath rw = (RewriteTask.IterableRewritePath) task;
                    if (rw.path() != null && !rw.path().equals(rootNode)) {
                        jlog.debug("skipping node: " + node);
                        continue;
                    }
                }
                if (!result && task instanceof RewriteTask.RewriteQuery) {
                    ((RewriteTask.RewriteQuery) task).rewriteQuery(node,
                            RewriteHandler.this.config, this.user);
                }
                else if (task instanceof RewriteTask.RewriteResult) {
                    ((RewriteTask.RewriteResult) task).rewriteResult(node);
                }

                if (node.isRemove()) {
                    node.buildRewrites(this.root.at("/" + rootNode));
                    break;
                }
                else
                    node.buildRewrites();
            }
            return node;
        }


        // fixme: merge with processNode!
        private void processFixedNode (JsonNode node,
                Collection<RewriteTask> tasks, boolean post)
                throws KustvaktException {
            for (RewriteTask task : tasks) {
                KoralNode next = KoralNode.wrapNode(node);
                if (task instanceof RewriteTask.RewriteNodeAt) {
                    RewriteTask.RewriteNodeAt rwa = (RewriteTask.RewriteNodeAt) task;
                    if ((rwa.at() != null && !node.at(rwa.at()).isMissingNode()))
                        next = next.at(rwa.at());
                }

                if (!post & task instanceof RewriteTask.RewriteQuery)
                    ((RewriteTask.RewriteQuery) task).rewriteQuery(next,
                            RewriteHandler.this.config, user);
                else if (task instanceof RewriteTask.RewriteResult)
                    ((RewriteTask.RewriteResult) task).rewriteResult(next);
                next.buildRewrites();

            }
        }


    }

    public void defaultRewriteConstraints () {
        this.add(FoundryInject.class);
    }
}
