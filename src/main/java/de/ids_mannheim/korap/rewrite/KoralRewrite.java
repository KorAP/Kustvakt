package de.ids_mannheim.korap.rewrite;

import java.util.LinkedHashMap;
import java.util.Map;

public class KoralRewrite {

    public Map<String, Object> map;

    public KoralRewrite () {
        this.map = new LinkedHashMap<>();
        this.map.put("@type", "koral:rewrite");
        this.map.put("src", "Kustvakt");
        this.map.put("editor", "Kustvakt");
    }

    public void setOperation (String op) {
        if (!op.startsWith("operation:"))
            op = "operation:" + op;
        this.map.put("operation", op);
    }

    public void setScope (String scope) {
        this.map.put("scope", scope);
    }
    
    public void setOriginal(Object original) {
        this.map.put("original", original);
    }
    
    public void setComment(String comment) {
        this.map.put("_comment", comment);
    }

}