package de.ids_mannheim.korap.oauth2.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.oauth2.entity.AccessToken;
import de.ids_mannheim.korap.oauth2.entity.Authorization;

@Repository
@Transactional
public class AccessTokenDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void storeAccessToken (Authorization authorization, String token) {
        AccessToken accessToken = new AccessToken();
        accessToken.setAuthorization(authorization);
        accessToken.setToken(token);
        accessToken.setScopes(authorization.getScopes());
        entityManager.persist(accessToken);
    }
}
