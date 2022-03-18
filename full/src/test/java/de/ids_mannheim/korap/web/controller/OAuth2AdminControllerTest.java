package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.dao.RefreshTokenDao;

public class OAuth2AdminControllerTest extends OAuth2TestBase {

    private String adminAuthHeader;
    @Autowired
    private RefreshTokenDao refreshDao;
    @Autowired
    private AccessTokenDao accessDao;

    public OAuth2AdminControllerTest () throws KustvaktException {
        adminAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("admin", "password");
    }

    @Test
    public void testCleanTokens () {
        int refreshTokensBefore =
                refreshDao.retrieveInvalidRefreshTokens().size();
        assertTrue(refreshTokensBefore > 0);

        int accessTokensBefore = accessDao.retrieveInvalidAccessTokens().size();
        assertTrue(accessTokensBefore > 0);

        resource().path(API_VERSION).path("oauth2").path("admin").path("token")
                .path("clean").header(Attributes.AUTHORIZATION, adminAuthHeader)
                .entity("token=adminToken").post(ClientResponse.class);
        assertEquals(0, refreshDao.retrieveInvalidRefreshTokens().size());
        assertEquals(0, accessDao.retrieveInvalidAccessTokens().size());
    }
}
