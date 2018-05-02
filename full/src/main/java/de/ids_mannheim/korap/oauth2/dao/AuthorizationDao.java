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
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.Authorization_;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Transactional
@Repository
public class AuthorizationDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void storeAuthorizationCode (String clientId, String userId,
            String code, Set<AccessScope> scopes, String redirectURI) {
        Authorization authCode = new Authorization();
        authCode.setCode(code);
        authCode.setClientId(clientId);
        authCode.setUserId(userId);
        authCode.setScopes(scopes);
        authCode.setRedirectURI(redirectURI);

        entityManager.persist(authCode);
        // what if unique fails
    }

    public Authorization retrieveAuthorizationCode (String code,
            String clientId) throws KustvaktException {
        ParameterChecker.checkStringValue(code, "code");
        ParameterChecker.checkStringValue(clientId, "client_id");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Authorization> query =
                builder.createQuery(Authorization.class);
        Root<Authorization> root = query.from(Authorization.class);

        Predicate restrictions = builder.and(
                builder.equal(root.get(Authorization_.code), code),
                builder.equal(root.get(Authorization_.clientId), clientId));

        query.select(root);
        query.where(restrictions);
        Query q = entityManager.createQuery(query);
        return (Authorization) q.getSingleResult();
    }

    public Authorization updateAuthorization (Authorization authorization) {
        authorization = entityManager.merge(authorization);
        return authorization;
    }
}
