package de.ids_mannheim.korap.oauth2.dao;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken_;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * Manages database queries and transactions regarding refresh tokens.
 * 
 * @author margaretha
 *
 */
@Repository
@Transactional
public class RefreshTokenDao {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private OAuth2ClientDao clientDao;

    public RefreshToken storeRefreshToken (String refreshToken, String userId,
            ZonedDateTime userAuthenticationTime, OAuth2Client client,
            Set<AccessScope> scopes) throws KustvaktException {
        ParameterChecker.checkStringValue(refreshToken, "refresh_token");
        // ParameterChecker.checkStringValue(userId, "username");
        ParameterChecker.checkObjectValue(client, "client");
        ParameterChecker.checkObjectValue(scopes, "scopes");

        ZonedDateTime now =
                ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE));

        RefreshToken token = new RefreshToken();
        token.setToken(refreshToken);
        token.setUserId(userId);
        token.setUserAuthenticationTime(userAuthenticationTime);
        token.setClient(client);
        token.setCreatedDate(now);
        token.setExpiryDate(now.plusSeconds(client.getRefreshTokenExpiry()));
        token.setScopes(scopes);

        entityManager.persist(token);
        return token;
    }

    public RefreshToken updateRefreshToken (RefreshToken token)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(token, "refresh_token");

        token = entityManager.merge(token);
        return token;
    }

    public RefreshToken retrieveRefreshToken (String token)
            throws KustvaktException {
        ParameterChecker.checkStringValue(token, "refresh_token");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RefreshToken> query =
                builder.createQuery(RefreshToken.class);
        Root<RefreshToken> root = query.from(RefreshToken.class);
        root.fetch(RefreshToken_.client);

        query.select(root);
        query.where(builder.equal(root.get(RefreshToken_.token), token));
        Query q = entityManager.createQuery(query);
        return (RefreshToken) q.getSingleResult();
    }

    public RefreshToken retrieveRefreshToken (String token, String username)
            throws KustvaktException {

        ParameterChecker.checkStringValue(token, "refresh_token");
        ParameterChecker.checkStringValue(username, "username");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RefreshToken> query =
                builder.createQuery(RefreshToken.class);

        Root<RefreshToken> root = query.from(RefreshToken.class);
        Predicate condition = builder.and(
                builder.equal(root.get(RefreshToken_.userId), username),
                builder.equal(root.get(RefreshToken_.token), token));

        query.select(root);
        query.where(condition);
        TypedQuery<RefreshToken> q = entityManager.createQuery(query);
        try {
            return q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }

    public List<RefreshToken> retrieveRefreshTokenByClientId (String clientId,
            String username) throws KustvaktException {
        ParameterChecker.checkStringValue(clientId, "client_id");
        OAuth2Client client = clientDao.retrieveClientById(clientId);

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RefreshToken> query =
                builder.createQuery(RefreshToken.class);
        Root<RefreshToken> root = query.from(RefreshToken.class);

        Predicate condition =
                builder.equal(root.get(RefreshToken_.client), client);
        if (username != null && !username.isEmpty()) {
            condition = builder.and(condition,
                    builder.equal(root.get(RefreshToken_.userId), username));
        }

        query.select(root);
        query.where(condition);
        TypedQuery<RefreshToken> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<RefreshToken> retrieveRefreshTokenByUser (String username,
            String clientId) throws KustvaktException {
        ParameterChecker.checkStringValue(username, "username");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RefreshToken> query =
                builder.createQuery(RefreshToken.class);

        Root<RefreshToken> root = query.from(RefreshToken.class);
        root.fetch(RefreshToken_.client);
        Predicate condition = builder.and(
                builder.equal(root.get(RefreshToken_.userId), username),
                builder.equal(root.get(RefreshToken_.isRevoked), false),
                builder.greaterThan(
                        root.<ZonedDateTime> get(RefreshToken_.expiryDate),
                        ZonedDateTime
                                .now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE))));
        if (clientId != null && !clientId.isEmpty()) {
            OAuth2Client client = clientDao.retrieveClientById(clientId);
            condition = builder.and(condition,
                    builder.equal(root.get(RefreshToken_.client), client));
        }

        query.select(root);
        query.where(condition);
        TypedQuery<RefreshToken> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public void deleteInvalidRefreshTokens () {
        List<RefreshToken> invalidRefreshTokens = retrieveInvalidRefreshTokens();
        invalidRefreshTokens.forEach(token -> entityManager.remove(token));
    }
    
    public List<RefreshToken> retrieveInvalidRefreshTokens () {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RefreshToken> query =
                builder.createQuery(RefreshToken.class);

        Root<RefreshToken> root = query.from(RefreshToken.class);
        Predicate condition = builder.or(
                builder.equal(root.get(RefreshToken_.isRevoked), true),
                builder.lessThan(
                        root.<ZonedDateTime> get(RefreshToken_.expiryDate),
                        ZonedDateTime
                                .now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE))));

        query.select(root);
        query.where(condition);
        TypedQuery<RefreshToken> q = entityManager.createQuery(query);
        return q.getResultList();
    }

}
