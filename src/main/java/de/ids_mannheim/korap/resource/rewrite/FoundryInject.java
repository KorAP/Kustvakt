package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.resource.LayerMapper;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 30/06/2015
 */
public class FoundryInject implements RewriteTask.RewriteNode {

    public FoundryInject() {
        super();
    }

    @Override
    public JsonNode preProcess(KoralNode node, KustvaktConfiguration config,
            User user) {
        LayerMapper mapper;
        if (user != null)
            mapper = new LayerMapper(config, user.getSettings());
        else
            mapper = new LayerMapper(config);

        if (node.get("@type").equals("koral:term") && !node.has("foundry")) {
            String layer;
            if (node.has("layer"))
                layer = node.get("layer");
            else
                layer = node.get("key");
            String foundry = mapper.findFoundry(layer);
            node.put("foundry", foundry);
        }
        return node.rawNode();
    }
}
