package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.dao.RefreshTokenDao;
import de.ids_mannheim.korap.utils.JsonUtils;

public class OAuth2AdminControllerTest extends OAuth2TestBase {

    private String adminAuthHeader;
    private String userAuthHeader;

    @Autowired
    private RefreshTokenDao refreshDao;
    @Autowired
    private AccessTokenDao accessDao;

    public OAuth2AdminControllerTest () throws KustvaktException {
        adminAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("admin", "password");
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
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
                .get(ClientResponse.class);

        assertEquals(0, refreshDao.retrieveInvalidRefreshTokens().size());
        assertEquals(0, accessDao.retrieveInvalidAccessTokens().size());
    }

    @Test
    public void testCleanTokenWithRevoke () throws KustvaktException {

        int accessTokensBefore = accessDao.retrieveInvalidAccessTokens().size();
        
        String code = requestAuthorizationCode(publicClientId, "", null,
                userAuthHeader);

        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, clientSecret, code);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        String accessToken = node.at("/access_token").asText();
        testRevokeToken(accessToken, publicClientId, null, ACCESS_TOKEN_TYPE);

        int accessTokensAfter = accessDao.retrieveInvalidAccessTokens().size();
        assertEquals(accessTokensAfter,accessTokensBefore+1);

        resource().path(API_VERSION).path("oauth2").path("admin").path("token")
                .path("clean").header(Attributes.AUTHORIZATION, adminAuthHeader)
                .get(ClientResponse.class);

        assertEquals(0, accessDao.retrieveInvalidAccessTokens().size());
    }
    
}
