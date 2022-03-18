package de.ids_mannheim.korap.oauth2.dao;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.KustvaktCacheable;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.AccessToken;
import de.ids_mannheim.korap.oauth2.entity.AccessToken_;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * Manages database queries and transactions regarding access tokens.
 * 
 * @author margaretha
 *
 */
@Repository
@Transactional
public class AccessTokenDao extends KustvaktCacheable {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private FullConfiguration config;
    @Autowired
    private OAuth2ClientDao clientDao;

    public AccessTokenDao () {
        super("access_token", "key:access_token");
    }

    public void storeAccessToken (String token, RefreshToken refreshToken,
            Set<AccessScope> scopes, String userId, String clientId,
            ZonedDateTime authenticationTime) throws KustvaktException {
        ParameterChecker.checkStringValue(token, "access_token");
        // ParameterChecker.checkObjectValue(refreshToken, "refresh
        // token");
        ParameterChecker.checkObjectValue(scopes, "scopes");
        // ParameterChecker.checkStringValue(userId, "username");
        ParameterChecker.checkStringValue(clientId, "client_id");
        ParameterChecker.checkObjectValue(authenticationTime,
                "authentication time");

        ZonedDateTime now =
                ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE));

        ZonedDateTime expiry;
        AccessToken accessToken = new AccessToken();

        if (refreshToken != null) {
            accessToken.setRefreshToken(refreshToken);
            expiry = now.plusSeconds(config.getAccessTokenExpiry());
        }
        else {
            expiry = now.plusSeconds(config.getAccessTokenLongExpiry());
        }

        OAuth2Client client = clientDao.retrieveClientById(clientId);

        accessToken.setCreatedDate(now);
        accessToken.setExpiryDate(expiry);
        accessToken.setToken(token);
        accessToken.setScopes(scopes);
        accessToken.setUserId(userId);
        accessToken.setClient(client);
        accessToken.setUserAuthenticationTime(authenticationTime);
        entityManager.persist(accessToken);
    }

    public AccessToken updateAccessToken (AccessToken accessToken)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(accessToken, "access_token");
        AccessToken cachedToken =
                (AccessToken) this.getCacheValue(accessToken.getToken());
        if (cachedToken != null) {
            this.removeCacheEntry(accessToken.getToken());
        }

        accessToken = entityManager.merge(accessToken);
        return accessToken;
    }

    public AccessToken retrieveAccessToken (String accessToken)
            throws KustvaktException {
        ParameterChecker.checkStringValue(accessToken, "access_token");
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
                    "Access token is invalid", OAuth2Error.INVALID_TOKEN);
        }
    }

    public AccessToken retrieveAccessToken (String accessToken, String username)
            throws KustvaktException {
        ParameterChecker.checkStringValue(accessToken, "access_token");
        ParameterChecker.checkStringValue(username, "username");
        AccessToken token = (AccessToken) this.getCacheValue(accessToken);
        if (token != null) {
            return token;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AccessToken> query =
                builder.createQuery(AccessToken.class);
        Root<AccessToken> root = query.from(AccessToken.class);

        Predicate condition = builder.and(
                builder.equal(root.get(AccessToken_.userId), username),
                builder.equal(root.get(AccessToken_.token), accessToken));

        query.select(root);
        query.where(condition);
        Query q = entityManager.createQuery(query);
        try {
            token = (AccessToken) q.getSingleResult();
            this.storeInCache(accessToken, token);
            return token;
        }
        catch (NoResultException e) {
            return null;
        }
    }

    public List<AccessToken> retrieveAccessTokenByClientId (String clientId,
            String username) throws KustvaktException {
        ParameterChecker.checkStringValue(clientId, "client_id");
        OAuth2Client client = clientDao.retrieveClientById(clientId);

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AccessToken> query =
                builder.createQuery(AccessToken.class);
        Root<AccessToken> root = query.from(AccessToken.class);

        Predicate condition =
                builder.equal(root.get(AccessToken_.client), client);
        if (username != null && !username.isEmpty()) {
            condition = builder.and(condition,
                    builder.equal(root.get(AccessToken_.userId), username));
        }

        query.select(root);
        query.where(condition);
        TypedQuery<AccessToken> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<AccessToken> retrieveAccessTokenByUser (String username,
            String clientId) throws KustvaktException {
        ParameterChecker.checkStringValue(username, "username");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AccessToken> query =
                builder.createQuery(AccessToken.class);

        Root<AccessToken> root = query.from(AccessToken.class);
        root.fetch(AccessToken_.client);
        Predicate condition = builder.and(
                builder.equal(root.get(AccessToken_.userId), username),
                builder.equal(root.get(AccessToken_.isRevoked), false),
                builder.greaterThan(
                        root.<ZonedDateTime> get(AccessToken_.expiryDate),
                        ZonedDateTime
                                .now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE))));
        if (clientId != null && !clientId.isEmpty()) {
            OAuth2Client client = clientDao.retrieveClientById(clientId);
            condition = builder.and(condition,
                    builder.equal(root.get(AccessToken_.client), client));
        }

        query.select(root);
        query.where(condition);
        TypedQuery<AccessToken> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public void deleteInvalidAccessTokens () {
        List<AccessToken> invalidAccessTokens = retrieveInvalidAccessTokens();
        invalidAccessTokens.forEach(token -> entityManager.remove(token));
    }
    
    public List<AccessToken> retrieveInvalidAccessTokens () {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AccessToken> query =
                builder.createQuery(AccessToken.class);

        Root<AccessToken> root = query.from(AccessToken.class);
        Predicate condition = builder.or(
                builder.equal(root.get(AccessToken_.isRevoked), true),
                builder.lessThan(
                        root.<ZonedDateTime> get(AccessToken_.expiryDate),
                        ZonedDateTime
                                .now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE))));

        query.select(root);
        query.where(condition);
        TypedQuery<AccessToken> q = entityManager.createQuery(query);
        return q.getResultList();
    }
}
