package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author hanl
 * @date 30/06/2015
 */
//todo: load rewritenode and rewritequery automatically from classpath, but namespaced from package
public class RewriteHandler {

    private static Logger jlog = KustvaktLogger.initiate(RewriteHandler.class);
    private Collection<RewriteTask.RewriteNode> node_processors;
    private Collection<RewriteTask.RewriteQuery> query_processors;

    public RewriteHandler() {
        this.node_processors = new HashSet<>();
        this.query_processors = new HashSet<>();
        // add defaults?!
    }

    public void add(RewriteTask.RewriteNode node) {
        this.node_processors.add(node);
    }

    public void add(RewriteTask.RewriteQuery node) {
        this.query_processors.add(node);
    }

    public void clear() {
        this.node_processors.clear();
        this.query_processors.clear();
    }

    private boolean process(JsonNode root, User user) {
        if (root.isObject()) {
            if (root.has("operands")) {
                JsonNode node = root.at("/operands");
                Iterator<JsonNode> it = node.elements();
                while (it.hasNext()) {
                    JsonNode n = it.next();
                    if (!process(n, user))
                        it.remove();
                }
            }else if (root.has("wrap")) {
                JsonNode node = root.at("/wrap");
                //todo: remove object nodes as well
                process(node, user);
            }else
                return processNode(root, user, this.node_processors);
        }
        return true;
    }

    public JsonNode apply(JsonNode root, User user) {
        for (JsonNode n : root)
            process(n, user);
        processNode(root, user, this.query_processors);
        return root;
    }

    public String apply(String json, User user) {
        return JsonUtils.toJSON(apply(JsonUtils.readTree(json), user));
    }

    private static boolean processNode(JsonNode node, User user,
            Collection<? extends RewriteTask> tasks) {
        KoralNode knode = KoralNode.getNode(node, user);
        for (RewriteTask task : tasks) {
            if (jlog.isDebugEnabled()) {
                jlog.debug("running node in processor " + node);
                jlog.debug("on processor " + task.getClass().toString());
            }
            task.rewrite(knode);
            if (knode.toRemove())
                break;
        }
        return !knode.toRemove();
    }

}
