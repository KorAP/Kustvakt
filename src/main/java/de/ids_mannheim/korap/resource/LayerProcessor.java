package de.ids_mannheim.korap.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

/**
 * @author hanl
 * @date 19/06/2015
 */
public class LayerProcessor extends NodeProcessor {

    private LayerMapper mapper;

    public LayerProcessor() {
        this.mapper = new LayerMapper();
    }

    @Override
    public JsonNode process(JsonNode node) {
        if (node.at("/query/wrap/@type").asText().equals("koral:term")) {
            JsonNode n = node.at("/query/wrap");
            if (n.path("foundry").isMissingNode()) {
                String layer = n.path("layer").asText();
                ObjectNode obj = (ObjectNode) n;
                obj.put("foundry", mapper.findFoundry(layer));
            }
        }else if (node.at("/query/wrap/@type").asText()
                .equals("koral:termGroup")) {
            processTermGroup(node.at("/query/wrap/operands"));
        }
        return node;
    }

    private JsonNode processTermGroup(JsonNode node) {
        Iterator<JsonNode> nodes = node.elements();
        while (nodes.hasNext()) {
            JsonNode n = nodes.next();
            if (n.path("@type").asText().equals("koral:termGroup"))
                n = processTermGroup(n.path("operands"));

            if (n.path("@type").asText().equals("koral:term") && n
                    .path("foundry").isMissingNode()) {
                String layer = n.path("layer").asText();
                ObjectNode obj = (ObjectNode) n;
                obj.put("foundry", mapper.findFoundry(layer));
            }
        }
        return node;
    }
}
