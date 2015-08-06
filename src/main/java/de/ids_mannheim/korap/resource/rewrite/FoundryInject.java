package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.resource.LayerMapper;

/**
 * @author hanl
 * @date 30/06/2015
 */
public class FoundryInject extends RewriteNode {

    private KustvaktConfiguration config;

    public FoundryInject(KustvaktConfiguration config) {
        this.config = config;
    }

    @Override
    public JsonNode rewrite(KoralNode node) {
        LayerMapper mapper;
        if (node.hasUser())
            mapper = new LayerMapper(config, node.getUser().getSettings());
        else
            mapper = new LayerMapper(config);

        if (node.rawNode().path("@type").asText().equals("koral:term") && !node
                .rawNode().has("foundry")) {
            String layer;
            if (node.rawNode().has("layer"))
                layer = node.rawNode().path("layer").asText();
            else
                layer = node.rawNode().path("key").asText();
            String foundry = mapper.findFoundry(layer);
            node.set("foundry", foundry);
        }
        return node.rawNode();
    }
}
