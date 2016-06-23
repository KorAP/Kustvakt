package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.config.AuthCodeInfo;
import de.ids_mannheim.korap.config.ClientInfo;
import de.ids_mannheim.korap.config.KustvaktCacheable;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.user.User;

/**
 * extends OAuthDb to allow temporary caching of tokens
 * and authorization codes.
 * Authorization codes are not persisted in db,
 * but stored in file of ehcache
 * 
 * @author hanl
 * @date 04/05/2015
 */
public class OAuth2Handler extends KustvaktCacheable {

    private OAuthDb oauthdb;

    public OAuth2Handler (PersistenceClient client) {
        super("auth_codes", "key:auth_codes");
        this.oauthdb = new OAuthDb(client);
    }


    // fixme: caching should not be obligatory here. alternative to caching if not available?
    public AuthCodeInfo getAuthorization (String code) {
        Object value = this.getCacheValue(code);
        if (value != null)
            return (AuthCodeInfo) value;
        return null;
    }


    public void authorize (AuthCodeInfo info, User user)
            throws KustvaktException {
        info.setUserId(user.getId());
        this.storeInCache(info.getCode(), info);
    }


    public boolean addToken (String code, String token, String refresh, int ttl)
            throws KustvaktException {
        Object o = this.getCacheValue(code);
        if (o != null) {
            AuthCodeInfo info = (AuthCodeInfo) o;
            this.removeCacheEntry(code);
            return oauthdb.addToken(token, refresh, info.getUserId(),
                    info.getClientId(), info.getScopes(), ttl);
        }
        return false;
    }


    public void exchangeToken (String refresh) {
        // todo:
    }

    public OAuthDb getPersistenceHandler(){
        return this.oauthdb;
    }

}
