package de.ids_mannheim.korap.oauth2.dao;

import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.config.KustvaktCacheable;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.AccessToken;
import de.ids_mannheim.korap.oauth2.entity.AccessToken_;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Repository
@Transactional
public class AccessTokenDao extends KustvaktCacheable {

    public AccessTokenDao () {
        super("access_token", "key:access_token");
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Deprecated
    public void storeAccessToken (Authorization authorization, String token)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(authorization, "Authorization");
        ParameterChecker.checkStringValue(token, "accessToken");

        AccessToken accessToken = new AccessToken();
        // accessToken.setAuthorization(authorization);
        accessToken.setUserId(authorization.getUserId());
        accessToken.setToken(token);
        accessToken.setScopes(authorization.getScopes());
        accessToken.setUserAuthenticationTime(
                authorization.getUserAuthenticationTime());
        entityManager.persist(accessToken);
    }

    public void storeAccessToken (String token, Set<AccessScope> scopes,
            String userId, String clientId, ZonedDateTime authenticationTime)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(scopes, "scopes");
        ParameterChecker.checkObjectValue(authenticationTime,
                "authentication time");
        AccessToken accessToken = new AccessToken();
        accessToken.setToken(token);
        accessToken.setScopes(scopes);
        accessToken.setUserId(userId);
        accessToken.setClientId(clientId);
        accessToken.setUserAuthenticationTime(authenticationTime);
        entityManager.persist(accessToken);
    }


    public AccessToken retrieveAccessToken (String accessToken)
            throws KustvaktException {

        AccessToken token = (AccessToken) this.getCacheValue(accessToken);
        if (token != null) {
            return token;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AccessToken> query =
                builder.createQuery(AccessToken.class);
        Root<AccessToken> root = query.from(AccessToken.class);
        query.select(root);
        query.where(builder.equal(root.get(AccessToken_.token), accessToken));
        Query q = entityManager.createQuery(query);
        try {
            token = (AccessToken) q.getSingleResult();
            this.storeInCache(accessToken, token);
            return token;
        }
        catch (NoResultException e) {
            throw new KustvaktException(StatusCodes.INVALID_ACCESS_TOKEN,
                    "Access token is not found");
        }
    }
}
