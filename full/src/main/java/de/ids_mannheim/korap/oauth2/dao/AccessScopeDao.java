package de.ids_mannheim.korap.oauth2.dao;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;

@Repository
@Transactional
public class AccessScopeDao {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<AccessScope> retrieveAccessScopes () {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AccessScope> query =
                builder.createQuery(AccessScope.class);
        Root<AccessScope> root = query.from(AccessScope.class);
        query.select(root);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public void storeAccessScopes (Set<OAuth2Scope> scopes) {
        List<AccessScope> existingScopes = retrieveAccessScopes();
        AccessScope newScope;
        for (OAuth2Scope scope : scopes) {
            newScope = new AccessScope(scope);
            if (!existingScopes.contains(newScope)) {
                entityManager.persist(newScope);
            }
        }

    }
}
