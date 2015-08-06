package de.ids_mannheim.korap.resource;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hanl
 * @date 19/06/2015
 */
public class RewriteProcessor {

    private KustvaktConfiguration config;
    private List<NodeProcessor> processors;

    public RewriteProcessor(KustvaktConfiguration config) {
        this.config = config;
        this.processors = new ArrayList<>();
        addProcessor(new LayerProcessor(config));
    }

    public JsonNode process(JsonNode node) {
        for (NodeProcessor p : this.processors)
            node = p.process(node);
        return node;
    }

    public String process(String json) {
        JsonNode node = JsonUtils.readTree(json);
        return JsonUtils.toJSON(process(node));
    }

    public void addProcessor(NodeProcessor processor) {
        this.processors.add(processor);
    }

}
