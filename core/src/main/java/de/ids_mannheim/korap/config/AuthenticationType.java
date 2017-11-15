package de.ids_mannheim.korap.config;

public enum AuthenticationType {
    LDAP, SHIBBOLETH, OAUTH2, OPENID, SESSION, BASIC;
    
    public String displayName () {
        return name().toLowerCase();
    }
}