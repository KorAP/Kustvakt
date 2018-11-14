package de.ids_mannheim.korap.constant;

import org.apache.commons.lang.StringUtils;

public enum TokenType {
    BASIC, API, SESSION, 
    // openid token, e.g. within oauth2 response (json body)
    ID_TOKEN,
    // OAuth2 access_token
    BEARER,
    // OAuth2 client
    CLIENT; 

    public String displayName () {
        return StringUtils.capitalize(name().toLowerCase());
    }
}