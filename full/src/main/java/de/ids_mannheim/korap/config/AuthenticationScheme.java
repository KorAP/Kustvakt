package de.ids_mannheim.korap.config;

import org.apache.commons.lang.WordUtils;

public enum AuthenticationScheme {
    // standard http
    BASIC, BEARER,
    // custom
    SESSION, API;
    
    public String displayName () {
        return WordUtils.capitalizeFully(name());
    }
}
