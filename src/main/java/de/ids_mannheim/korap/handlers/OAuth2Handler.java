package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.config.AuthCodeInfo;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.user.User;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * extends OAuthDb to allow temporary caching of tokens
 * and authorizations (authorizations are not persisted in db)
 *
 * @author hanl
 * @date 04/05/2015
 */
public class OAuth2Handler extends OAuthDb {

    private Cache cache;

    public OAuth2Handler(PersistenceClient client) {
        super(client);
        this.cache = CacheManager.getInstance().getCache("auth_codes");
    }

    public AuthCodeInfo getAuthorization(String code) {
        Element e = this.cache.get(code);
        if (e != null)
            return (AuthCodeInfo) e.getObjectValue();
        return null;
    }

    public void authorize(AuthCodeInfo code, User user)
            throws KustvaktException {
        code.setUserId(user.getId());
        cache.put(new Element(code.getCode(), code));
    }

    public boolean addToken(String code, String token, String refresh, int ttl)
            throws KustvaktException {
        Element e = cache.get(code);
        if (e != null) {
            AuthCodeInfo info = (AuthCodeInfo) e.getObjectValue();
            cache.remove(code);
            return super.addToken(token, refresh, info.getUserId(), info.getClientId(),
                    info.getScopes(), ttl);
        }
        return false;
    }

    public void exchangeToken(String refresh) {

    }

}
