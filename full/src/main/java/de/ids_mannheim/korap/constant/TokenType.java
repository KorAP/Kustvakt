package de.ids_mannheim.korap.constant;

public enum TokenType {
    BASIC, API, SESSION, 
    // openid token, e.g. within oauth2 response (json body)
    ID_TOKEN,
    // OAuth2 access_token, practically formulated identical as TokenType.API
    BEARER,
    // OAuth2 client
    CLIENT; 

    public String displayName () {
        return name().toLowerCase();
    }
}