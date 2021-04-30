package de.ids_mannheim.korap.oauth2.dao;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.Authorization_;
import de.ids_mannheim.korap.utils.ParameterChecker;

/**
 * Implementations of {@link AuthorizationDao} managing database
 * queries and transactions regarding OAuth2 authorizations.
 * 
 * @author margaretha
 *
 */
@Transactional
@Repository
public class AuthorizationDaoImpl implements AuthorizationDao {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private FullConfiguration config;

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

        entityManager.persist(authorization);
        // what if unique fails
        return authorization;
    }

    public Authorization retrieveAuthorizationCode (String code)
            throws KustvaktException {
        ParameterChecker.checkStringValue(code, "code");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Authorization> query =
                builder.createQuery(Authorization.class);
        Root<Authorization> root = query.from(Authorization.class);

        Predicate restrictions =
                builder.equal(root.get(Authorization_.code), code);

        query.select(root);
        query.where(restrictions);
        Query q = entityManager.createQuery(query);
        try {
            return (Authorization) q.getSingleResult();
        }
        catch (Exception e) {
            throw new KustvaktException(StatusCodes.INVALID_AUTHORIZATION,
                    "Invalid authorization: " + e.getMessage(),
                    OAuth2Error.INVALID_REQUEST);
        }
    }

    public Authorization updateAuthorization (Authorization authorization)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(authorization, "authorization");
        authorization = entityManager.merge(authorization);
        return authorization;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Authorization> retrieveAuthorizationsByClientId (
            String clientId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Authorization> query =
                builder.createQuery(Authorization.class);
        Root<Authorization> root = query.from(Authorization.class);

        Predicate restrictions =
                builder.equal(root.get(Authorization_.clientId), clientId);

        query.select(root);
        query.where(restrictions);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }
}
