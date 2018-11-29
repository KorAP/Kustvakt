package de.ids_mannheim.korap.oauth2.dao;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client_;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken_;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Repository
@Transactional
public class RefreshTokenDao {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private FullConfiguration config;
    @Autowired
    private OAuth2ClientDao clientDao;

    public RefreshToken storeRefreshToken (String refreshToken, String userId,
            ZonedDateTime userAuthenticationTime, String clientId,
            Set<AccessScope> scopes) throws KustvaktException {
        ParameterChecker.checkStringValue(refreshToken, "refresh token");
        // ParameterChecker.checkStringValue(userId, "username");
        ParameterChecker.checkStringValue(clientId, "client_id");
        ParameterChecker.checkObjectValue(scopes, "scopes");

        ZonedDateTime now =
                ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE));
        OAuth2Client client = clientDao.retrieveClientById(clientId);

        RefreshToken token = new RefreshToken();
        token.setToken(refreshToken);
        token.setUserId(userId);
        token.setUserAuthenticationTime(userAuthenticationTime);
        token.setClient(client);
        token.setCreatedDate(now);
        token.setExpiryDate(now.plusSeconds(config.getRefreshTokenExpiry()));
        token.setScopes(scopes);

        entityManager.persist(token);
        return token;
    }

    public RefreshToken retrieveRefreshToken (String token)
            throws KustvaktException {
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

    public List<RefreshToken> retrieveRefreshTokenByClientId (String clientId)
            throws KustvaktException {
        ParameterChecker.checkStringValue(clientId, "client_id");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RefreshToken> query =
                builder.createQuery(RefreshToken.class);
        Root<RefreshToken> root = query.from(RefreshToken.class);
        Join<RefreshToken, OAuth2Client> client =
                root.join(RefreshToken_.client);
        query.select(root);
        query.where(builder.equal(client.get(OAuth2Client_.id), clientId));
        TypedQuery<RefreshToken> q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public RefreshToken updateRefreshToken (RefreshToken token)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(token, "refresh_token");

        token = entityManager.merge(token);
        return token;
    }

    // public List<RefreshToken> retrieveRefreshTokenByUser (String
    // username)
    // throws KustvaktException {
    // ParameterChecker.checkStringValue(username, "username");
    //
    // CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    // CriteriaQuery<RefreshToken> query =
    // builder.createQuery(RefreshToken.class);
    //
    // Root<RefreshToken> root = query.from(RefreshToken.class);
    // Predicate condition = builder.and(
    // builder.equal(root.get(RefreshToken_.userId), username),
    // builder.equal(root.get(RefreshToken_.isRevoked), false),
    // builder.greaterThan(
    // root.<ZonedDateTime> get(RefreshToken_.expiryDate),
    // ZonedDateTime
    // .now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE)))
    // );
    // query.select(root);
    // query.where(condition);
    // TypedQuery<RefreshToken> q = entityManager.createQuery(query);
    // return q.getResultList();
    // }
}
