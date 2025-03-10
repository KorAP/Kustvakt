package de.ids_mannheim.korap.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.rewrite.KoralNode;
import de.ids_mannheim.korap.rewrite.RewriteIdentifier;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl
 * @date 21/10/2015
 */
// todo: complete tests
public class KoralNodeTest {

    // todo: 21.10.15 --> e.g. injection does not tell you if an entire node was injected, or just a value!
    @Test
    public void addToNode () {
        ObjectNode node = JsonUtils.createObjectNode();
        KoralNode knode = KoralNode.wrapNode(node);
        knode.put("value_1", "setting_1");
        assertEquals(knode.rawNode().toString(), "{\"value_1\":\"setting_1\"}");
    }

    @Test
    public void removeFromNode () {
        ObjectNode node = JsonUtils.createObjectNode();
        node.put("value_1", "setting_1");
        KoralNode knode = KoralNode.wrapNode(node);
        knode.remove("value_1",
                new RewriteIdentifier("value_1", "", "settings_1"));
        assertEquals(knode.rawNode().toString(), "{}");
    }

    @Test
    public void replaceObject () {
        ObjectNode node = JsonUtils.createObjectNode();
        node.put("value_1", "setting_1");
        KoralNode knode = KoralNode.wrapNode(node);
        knode.replace("value_1", "settings_2",
                new RewriteIdentifier("value_1", "setting_1", ""));
        assertEquals(knode.rawNode().toString(),
                "{\"value_1\":\"settings_2\"}");
    }

    // todo: 21.10.15 --> if a node is injected, that node must contain a "rewrites" reference?!
    @Test
    public void addNodeToKoral () {}
}
