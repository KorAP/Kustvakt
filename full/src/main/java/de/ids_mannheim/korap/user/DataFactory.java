package de.ids_mannheim.korap.user;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.validator.Validator;

/**
 * EM: util class
 * 
 * @author hanl, margaretha
 * @date 27/01/2016
 */
public abstract class DataFactory {

    private static DataFactory factory;

    private DataFactory () {}

    public static DataFactory getFactory () {
        if (factory == null)
            factory = new DefaultFactory();
        return factory;
    }


    /**
     * if data string null, returns an empty data holding object
     * 
     * @param data
     * @return
     */
    public abstract Object convertData (String data);


    public abstract int size (Object data);


    public abstract Set<String> keys (Object data);


    public abstract Collection<Object> values (Object data);

    public abstract Object validate(Object data, Validator validator) throws KustvaktException;

    @Deprecated
    public abstract Map<String, Object> fields (Object data);


    public abstract Object getValue (Object data, String pointer);


    public abstract boolean addValue (Object data, String field, Object value);


    public abstract boolean removeValue (Object data, String field);


    public abstract String toStringValue (Object data) throws KustvaktException;

    public abstract Object filter(Object data, String ... keys);

    public boolean checkDataType (Object data) {
        throw new RuntimeException("Wrong data type for factory setting!");
    }


    /**
     * updates data1 with values from data2
     * 
     * @param data1
     *            data object that needs update
     * @param data2
     *            values that update data1
     * @return
     */
    public abstract Object merge (Object data1, Object data2);



    private static class DefaultFactory extends DataFactory {

        @Override
        public Object convertData (String data) {
            if (data == null)
                return JsonUtils.createObjectNode();
            try {
                return JsonUtils.readTree(data);
            }
            catch (KustvaktException e) {
                return null;
            }
        }


        @Override
        public int size (Object data) {
            if (checkDataType(data))
                return ((JsonNode) data).size();
            return -1;
        }


        @Override
        public Set<String> keys (Object data) {
            Set<String> keys = new HashSet<>();
            if (checkDataType(data) && ((JsonNode) data).isObject()) {
                Iterator<String> it = ((JsonNode) data).fieldNames();
                while (it.hasNext())
                    keys.add((String) it.next());
            }
            return keys;
        }


        @Override
        public Collection<Object> values (Object data) {
            return new HashSet<>();
        }

        @Override
        public Object validate(Object data, Validator validator) throws KustvaktException {
            if (checkDataType(data) && ((JsonNode) data).isObject()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mdata = JsonUtils.read(toStringValue(data), HashMap.class);
                    return validator.validateMap(mdata);
                } catch (IOException e) {
                    // do nothing
                }
            }
            return JsonUtils.createObjectNode();
        }


        @Override
        public Map<String, Object> fields (Object data) {
            return new HashMap<>();
        }


        @Override
        public Object getValue (Object data, String key) {
            if (checkDataType(data)) {
                JsonNode value;
                if (key.startsWith("/"))
                    value = ((JsonNode) data).at(key);
                else
                    value = ((JsonNode) data).path(key);

                if (value.canConvertToInt())
                    return value.asInt();
                else if (value.isBoolean())
                    return value.asBoolean();
                else if (value.isTextual())
                    return value.asText();
            }
            return null;
        }


        //fixme: test that this works with different types
        @Override
        public boolean addValue (Object data, String field, Object value) {
            if (checkDataType(data)) {
                if (((JsonNode) data).isObject()) {
                    ObjectNode node = (ObjectNode) data;
                    if (value instanceof String)
                        node.put(field, (String) value);
                    if (value instanceof Boolean)
                        node.put(field, (Boolean) value);
                    if (value instanceof Integer)
                        node.put(field, (Integer) value);
                    if (value instanceof JsonNode)
                        node.set(field, (JsonNode) value);
                    // EM: added
                    if (value instanceof Collection<?>){
                        Collection<?> list = (Collection<?>) value;
                        ArrayNode arrayNode = JsonUtils.createArrayNode();
                        for (Object o : list){
                            addValue(arrayNode, null, o);
                        }
                        node.set(field,arrayNode);
                    }
                    return true;
                }
                else if (((JsonNode) data).isArray()) {
                    ArrayNode node = (ArrayNode) data;
                    if (value instanceof String)
                        node.add((String) value);
                    if (value instanceof Boolean)
                        node.add((Boolean) value);
                    if (value instanceof Integer)
                        node.add((Integer) value);
                    return true;
                }
            }
            return false;
        }


        @Override
        public boolean removeValue (Object data, String field) {
            if (checkDataType(data) && ((JsonNode) data).isObject()) {
                ObjectNode node = (ObjectNode) data;
                node.remove(field);
                return true;
            }
            return false;
        }


        @Override
        public String toStringValue (Object data) throws KustvaktException {
            if (data instanceof JsonNode)
                return JsonUtils.toJSON(data);
            return data.toString();
        }

        @Override
        public Object filter(Object data, String... keys) {
            if (checkDataType(data) && ((JsonNode) data).isObject()) {
                ObjectNode node = ((JsonNode) data).deepCopy();
                return node.retain(keys);
            }
            return JsonUtils.createObjectNode();
        }


        @Override
        public boolean checkDataType (Object data) {
            if (!(data instanceof JsonNode))
                super.checkDataType(data);
            return true;
        }


        @Override
        public Object merge (Object data1, Object data2) {
            if (checkDataType(data1) && checkDataType(data2)) {
                if (((JsonNode) data1).isObject()
                        && ((JsonNode) data2).isObject()) {
                    ((ObjectNode) data1).setAll((ObjectNode) data2);
                }
                else if (((JsonNode) data1).isArray()
                        && ((JsonNode) data2).isArray()) {
                    ((ArrayNode) data1).addAll((ArrayNode) data2);
                }
            }
            return data1;
        }

    }
}
