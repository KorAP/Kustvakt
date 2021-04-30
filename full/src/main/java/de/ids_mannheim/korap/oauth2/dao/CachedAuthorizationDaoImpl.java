package de.ids_mannheim.korap.oauth2.dao;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.KustvaktCacheable;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.utils.ParameterChecker;
import net.sf.ehcache.Element;

/**
 * Implementations of {@link AuthorizationDao} using a cache instead
 * of a database.
 * 
 * @author margaretha
 *
 */
public class CachedAuthorizationDaoImpl extends KustvaktCacheable
        implements AuthorizationDao {

    @Autowired
    private FullConfiguration config;

    public CachedAuthorizationDaoImpl () {
        super("authorization", "key:authorization");
    }

    @Override
    public Authorization storeAuthorizationCode (String clientId, String userId,
            String code, Set<AccessScope> scopes, String redirectURI,
            ZonedDateTime authenticationTime, String nonce)
            throws KustvaktException {
        ParameterChecker.checkStringValue(clientId, "client_id");
        ParameterChecker.checkStringValue(userId, "user_id");
        ParameterChecker.checkStringValue(code, "authorization_code");
        ParameterChecker.checkCollection(scopes, "scopes");
        ParameterChecker.checkObjectValue(authenticationTime,
                "user_authentication_time");

        Authorization authorization = new Authorization();
        authorization.setCode(code);
        authorization.setClientId(clientId);
        authorization.setUserId(userId);
        authorization.setScopes(scopes);
        authorization.setRedirectURI(redirectURI);
        authorization.setUserAuthenticationTime(authenticationTime);
        authorization.setNonce(nonce);

        ZonedDateTime now =
                ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE));
        authorization.setCreatedDate(now);
        authorization.setExpiryDate(
                now.plusSeconds(config.getAuthorizationCodeExpiry()));

        this.storeInCache(code, authorization);
        return authorization;
    }

    @Override
    public Authorization retrieveAuthorizationCode (String code)
            throws KustvaktException {

        Object auth = this.getCacheValue(code);
        if (auth != null) {
            return (Authorization) auth;
        }
        else {
            throw new KustvaktException(StatusCodes.INVALID_AUTHORIZATION,
                    "Authorization is invalid.", OAuth2Error.INVALID_REQUEST);
        }
    }

    @Override
    public Authorization updateAuthorization (Authorization authorization)
            throws KustvaktException {

        this.storeInCache(authorization.getCode(), authorization);
        Authorization auth =
                (Authorization) this.getCacheValue(authorization.getCode());
        return auth;
    }

    @Override
    public List<Authorization> retrieveAuthorizationsByClientId (
            String clientId) {
        List<Authorization> authList = new ArrayList<>();

        Map<Object, Element> map = getAllCacheElements();
        for (Object key : map.keySet()) {
            Authorization auth = (Authorization) map.get(key).getObjectValue();
            if (auth.getClientId().equals(clientId)) {
                authList.add(auth);
            }
        }
        return authList;
    }

}
