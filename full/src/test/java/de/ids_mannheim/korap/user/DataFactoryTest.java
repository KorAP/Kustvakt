package de.ids_mannheim.korap.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Taken from UserdataTest
 *
 * @author hanl
 * @date 27/01/2016
 */
@DisplayName("Data Factory Test")
class DataFactoryTest {

    @Test
    @DisplayName("Test Data Factory Add")
    void testDataFactoryAdd() throws KustvaktException {
        String data = "{}";
        Object node = JsonUtils.readTree(data);
        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));
        data = "[]";
        node = JsonUtils.readTree(data);
        factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));
    }

    @Test
    @DisplayName("Test Data Factory Get")
    void testDataFactoryGet() throws KustvaktException {
        String data = "{}";
        Object node = JsonUtils.readTree(data);
        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));
        Object value = factory.getValue(node, "field_1");
        assertEquals(value, "value_1");
        value = factory.getValue(node, "field_2");
        assertEquals(20, value);
        value = factory.getValue(node, "field_3");
        assertEquals(true, value);
        data = "[]";
        node = JsonUtils.readTree(data);
        assertTrue(factory.addValue(node, "", "value_2"));
        assertTrue(factory.addValue(node, "", 10));
        assertTrue(factory.addValue(node, "", false));
        value = factory.getValue(node, "/0");
        assertEquals(value, "value_2");
        value = factory.getValue(node, "/1");
        assertEquals(10, value);
        value = factory.getValue(node, "/2");
        assertEquals(false, value);
    }

    @Test
    @DisplayName("Test Data Factory Merge")
    void testDataFactoryMerge() throws KustvaktException {
        String data = "{}";
        Object node = JsonUtils.readTree(data);
        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));
        data = "{}";
        Object node2 = JsonUtils.readTree(data);
        assertTrue(factory.addValue(node2, "field_1", "value_new"));
        assertTrue(factory.addValue(node2, "field_2", "value_next"));
        assertTrue(factory.addValue(node2, "field_4", "value_2"));
        assertTrue(factory.addValue(node2, "field_7", "value_3"));
        JsonNode node_new = (JsonNode) factory.merge(node, node2);
        assertEquals(node_new.path("field_1").asText(), "value_new");
        assertEquals(node_new.path("field_2").asText(), "value_next");
        assertEquals(true, node_new.path("field_3").asBoolean());
        assertEquals(node_new.path("field_4").asText(), "value_2");
        assertEquals(node_new.path("field_7").asText(), "value_3");
    }

    @Test
    @DisplayName("Test Data Factory Keys")
    void testDataFactoryKeys() throws KustvaktException {
        String data = "{}";
        Object node = JsonUtils.readTree(data);
        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));
        assertEquals(3, factory.size(node));
        assertEquals(3, factory.keys(node).size());
    }

    @Test
    @Disabled
    @DisplayName("Test Data Factory Remove")
    void testDataFactoryRemove() throws KustvaktException {
        String data = "{}";
        Object node = JsonUtils.readTree(data);
        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));
        Object value = factory.getValue(node, "field_1");
        assertEquals(value, "value_1");
        value = factory.getValue(node, "field_2");
        assertEquals(20, value);
        value = factory.getValue(node, "field_3");
        assertEquals(true, value);
        assertTrue(factory.removeValue(node, "field_1"));
        assertTrue(factory.removeValue(node, "field_2"));
        assertTrue(factory.removeValue(node, "field_3"));
        assertNotNull(node);
        assertEquals(node.toString(), "{}");
        data = "[]";
        node = JsonUtils.readTree(data);
        assertTrue(factory.addValue(node, "", "value_2"));
        assertTrue(factory.addValue(node, "", 10));
        assertTrue(factory.addValue(node, "", false));
        value = factory.getValue(node, "/0");
        assertEquals(value, "value_2");
        value = factory.getValue(node, "/1");
        assertEquals(10, value);
        value = factory.getValue(node, "/2");
        assertEquals(false, value);
        // fixme: cannot be removed
        assertTrue(factory.removeValue(node, "0"));
        assertTrue(factory.removeValue(node, "1"));
        assertTrue(factory.removeValue(node, "2"));
        assertNotNull(node);
        assertEquals(node.toString(), "[]");
    }

    @Test
    @DisplayName("Test Data Factory Embedded Property")
    void testDataFactoryEmbeddedProperty() throws KustvaktException {
        String data = "{}";
        JsonNode node = JsonUtils.readTree(data);
        DataFactory factory = DataFactory.getFactory();
        assertTrue(factory.addValue(node, "field_1", "value_1"));
        assertTrue(factory.addValue(node, "field_2", 20));
        assertTrue(factory.addValue(node, "field_3", true));
        ArrayNode array = JsonUtils.createArrayNode();
        array.add(10);
        array.add("v1");
        array.add("v2");
        factory.addValue(node, "field_3", array);
        assertNotNull(node);
        assertEquals(10, node.at("/field_3/0").asInt());
        assertEquals(node.at("/field_3/1").asText(), "v1");
        assertEquals(node.at("/field_3/2").asText(), "v2");
    }
}
