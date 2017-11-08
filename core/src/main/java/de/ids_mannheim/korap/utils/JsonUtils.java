package de.ids_mannheim.korap.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

/**
 * @author hanl
 * @date 28/01/2014
 */
public class JsonUtils {
    private static ObjectMapper mapper = new ObjectMapper();

    private JsonUtils () {}


    public static String toJSON (Object values) {
        try {
            return mapper.writeValueAsString(values);
        }
        catch (JsonProcessingException e) {
            return e.getMessage();
        }
    }


    public static JsonNode readTree (String json) throws KustvaktException {
        try {
            return mapper.readTree(json);
        }
        catch (IOException e) {
            throw new KustvaktException(StatusCodes.DESERIALIZATION_FAILED,
                    "Failed deserializing json object: " + json, json, e);
        }
    }


    public static ObjectNode createObjectNode () {
        return mapper.createObjectNode();
    }


    public static ArrayNode createArrayNode () {
        return mapper.createArrayNode();
    }


    public static JsonNode valueToTree (Object value) {
        return mapper.valueToTree(value);
    }

    public static <T> T convert (JsonNode json, Class<T> cl)
            throws IOException {
        return mapper.convertValue(json, cl);
    }

    public static <T> T read (String json, Class<T> cl) throws IOException {
        return mapper.readValue(json, cl);
    }


    public static <T> T readFile (String path, Class<T> clazz)
            throws IOException {
        return mapper.readValue(new File(path), clazz);
    }


    public static void writeFile (String path, String content)
            throws IOException {
        mapper.writeValue(new File(path), content);
    }


    public static <T> T convertToClass (String json, Class<T> cl) throws KustvaktException {
        T t = null;
        try {
            t = mapper.readValue(json, cl);
        }
        catch (IOException e) {
            throw new KustvaktException(StatusCodes.DESERIALIZATION_FAILED,
                    "Failed deserializing json object: " + json, json, e);
        }
        return t;
    }


    public static List<Map<String, Object>> convertToList (String json)
            throws JsonProcessingException, KustvaktException {
        List d = new ArrayList();
        JsonNode node = JsonUtils.readTree(json);
        if (node.isArray()) {
            Iterator<JsonNode> nodes = node.iterator();
            while (nodes.hasNext()) {
                Map<String, Object> map =
                        mapper.treeToValue(nodes.next(), Map.class);
                d.add(map);
            }
        }
        else if (node.isObject()) {
            Map<String, Object> map = mapper.treeToValue(node, Map.class);
            d.add(map);
        }
        return d;
    }

}
