package de.ids_mannheim.korap.exceptions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author hanl
 * @date 03/12/2014
 */
public class Message implements Cloneable {

    ObjectMapper mapper = new ObjectMapper();
    private String msg;
    private int code = 0;
    private LinkedList<String> parameters;

    public Message(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Message() {
    }

    @JsonIgnore
    public Message setMessage(String msg) {
        this.msg = msg;
        return this;
    }

    @JsonIgnore
    public String getMessage() {
        return this.msg;
    }

    @JsonIgnore
    public Message setCode(int code) {
        this.code = code;
        return this;
    }

    @JsonIgnore
    public int getCode() {
        return this.code;
    }

    public Message addParameter(String param) {
        if (this.parameters == null) {
            this.parameters = new LinkedList();
        }

        this.parameters.add(param);
        return this;
    }

    public Object clone() throws CloneNotSupportedException {
        Message clone = new Message();
        if (this.msg != null) {
            clone.msg = this.msg;
        }

        clone.code = this.code;
        if (this.parameters != null) {
            Iterator i$ = this.parameters.iterator();

            while (i$.hasNext()) {
                String p = (String) i$.next();
                clone.addParameter(p);
            }
        }

        return clone;
    }

    public JsonNode toJSONnode() {
        ArrayNode message = this.mapper.createArrayNode();
        if (this.code != 0) {
            message.add(this.getCode());
        }

        message.add(this.getMessage());
        if (this.parameters != null) {
            Iterator i$ = this.parameters.iterator();

            while (i$.hasNext()) {
                String p = (String) i$.next();
                message.add(p);
            }
        }

        return message;
    }

    public String toJSON() {
        String msg = "";

        try {
            return this.mapper.writeValueAsString(this.toJSONnode());
        } catch (Exception var3) {
            msg = ", \"" + var3.getLocalizedMessage() + "\"";
            return "[620, \"Unable to generate JSON\"" + msg + "]";
        }
    }
}
