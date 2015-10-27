package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.resource.LayerMapper;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 30/06/2015
 */
public class FoundryInject extends RewriteTask.RewriteNode {

    public FoundryInject() {
        super();
    }

    @Override
    public JsonNode rewrite(KoralNode node, KustvaktConfiguration config,
            User user) {
        LayerMapper mapper;
        if (user != null)
            mapper = new LayerMapper(config, user.getSettings());
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
