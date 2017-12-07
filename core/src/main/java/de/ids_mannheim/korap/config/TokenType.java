package de.ids_mannheim.korap.config;

public enum TokenType {
    BASIC, API, SESSION, 
    // openid token, e.g. within oauth2 response (json body)
    ID_TOKEN,
    // OAuth2 access_token, practically formulated identical as TokenType.API
    BEARER; 

    public String displayName () {
        return name().toLowerCase();
    }
}