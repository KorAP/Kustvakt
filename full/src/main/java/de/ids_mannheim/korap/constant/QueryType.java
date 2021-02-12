package de.ids_mannheim.korap.constant;

import org.apache.commons.lang.StringUtils;

public enum QueryType {

    QUERY,
    VIRTUAL_CORPUS;
    
    public String displayName () {
        return StringUtils.capitalize(name().toLowerCase().replace("_", " "));
    }
}
