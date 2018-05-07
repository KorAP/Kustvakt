package de.ids_mannheim.korap.oauth2.dao;

import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.AccessToken;
import de.ids_mannheim.korap.oauth2.entity.AccessToken_;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Repository
@Transactional
public class AccessTokenDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void storeAccessToken (Authorization authorization, String token)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(authorization, "Authorization");
        ParameterChecker.checkStringValue(token, "accessToken");

        AccessToken accessToken = new AccessToken();
        accessToken.setAuthorization(authorization);
        accessToken.setUserId(authorization.getUserId());
        accessToken.setToken(token);
        accessToken.setScopes(authorization.getScopes());
        entityManager.persist(accessToken);
    }

    public void storeAccessToken (String token, Set<AccessScope> scopes,
            String userId) throws KustvaktException {
        ParameterChecker.checkObjectValue(scopes, "scopes");
        AccessToken accessToken = new AccessToken();
        accessToken.setToken(token);
        accessToken.setScopes(scopes);
        accessToken.setUserId(userId);
        entityManager.persist(accessToken);
    }


    public AccessToken retrieveAccessToken (String accessToken) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AccessToken> query =
                builder.createQuery(AccessToken.class);
        Root<AccessToken> root = query.from(AccessToken.class);
        query.select(root);
        query.where(builder.equal(root.get(AccessToken_.token), accessToken));
        Query q = entityManager.createQuery(query);
        return (AccessToken) q.getSingleResult();
    }
}
