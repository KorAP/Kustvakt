package de.ids_mannheim.korap.oauth2.dao;

import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.Authorization_;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Transactional
@Repository
public class AuthorizationDao {

    @PersistenceContext
    private EntityManager entityManager;

    public Authorization storeAuthorizationCode (String clientId, String userId,
            String code, Set<AccessScope> scopes, String redirectURI)
            throws KustvaktException {
        ParameterChecker.checkStringValue(clientId, "client_id");
        ParameterChecker.checkStringValue(userId, "userId");
        ParameterChecker.checkStringValue(code, "authorization code");
        ParameterChecker.checkCollection(scopes, "scopes");
        
        Authorization authCode = new Authorization();
        authCode.setCode(code);
        authCode.setClientId(clientId);
        authCode.setUserId(userId);
        authCode.setScopes(scopes);
        authCode.setRedirectURI(redirectURI);

        entityManager.persist(authCode);
        // what if unique fails
        return authCode;
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
}
