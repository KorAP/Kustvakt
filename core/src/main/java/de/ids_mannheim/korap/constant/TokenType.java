package de.ids_mannheim.korap.constant;

import org.apache.commons.lang.StringUtils;

import de.ids_mannheim.korap.security.context.TokenContext;

/** Defines the types of authentication tokens. Token types are used to
 * create {@link TokenContext} and determine which authentication provider
 * must be used to create a TokenContext and parse given tokens. 
 * 
 * @author margaretha
 *
 */
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