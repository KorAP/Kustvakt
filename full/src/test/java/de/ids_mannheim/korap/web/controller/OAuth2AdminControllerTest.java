package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.dao.RefreshTokenDao;
import de.ids_mannheim.korap.utils.JsonUtils;

public class OAuth2AdminControllerTest extends OAuth2TestBase {

    private String username = "OAuth2AdminControllerTest";
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

    private Response updateClientPrivilege (String username,
            Form form)
            throws ProcessingException,
            KustvaktException {
        Response response = target().path(API_VERSION).path("admin")
                .path("oauth2").path("client").path("privilege")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));

        return response;
    }
    
    private Response updateClientPrivilegeWithAdminToken (String clientId)
            throws ProcessingException,
            KustvaktException {
        
        Form form = new Form();
        form.param("client_id", clientId);
        form.param("super", Boolean.toString(false));
        form.param("token", "secret"); //adminToken
        
        Response response = target().path(API_VERSION).path("admin")
                .path("oauth2").path("client").path("privilege")
                .request()
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));

        return response;
    }
    
    
    private void testUpdateClientPriviledgeUnauthorized (Form form)
            throws ProcessingException, KustvaktException {
        Response response = updateClientPrivilege(username, form);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testCleanExpiredTokensUsingAdminToken () {
        int refreshTokensBefore =
                refreshDao.retrieveInvalidRefreshTokens().size();
        assertTrue(refreshTokensBefore > 0);

        int accessTokensBefore = accessDao.retrieveInvalidAccessTokens().size();
        assertTrue(accessTokensBefore > 0);

        Form form = new Form();
        form.param("token", "secret");
        
        target().path(API_VERSION).path("admin").path("oauth2").path("token")
                .path("clean")
                .request()
                .post(Entity.form(form));

        assertEquals(0, refreshDao.retrieveInvalidRefreshTokens().size());
        assertEquals(0, accessDao.retrieveInvalidAccessTokens().size());
    }

    @Test
    public void testCleanRevokedTokens () throws KustvaktException {

        int accessTokensBefore = accessDao.retrieveInvalidAccessTokens().size();
        String code = requestAuthorizationCode(publicClientId, userAuthHeader);

        Response response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, clientSecret, code);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        String accessToken = node.at("/access_token").asText();
        testRevokeToken(accessToken, publicClientId, null, ACCESS_TOKEN_TYPE);

        int accessTokensAfter = accessDao.retrieveInvalidAccessTokens().size();
        assertEquals(accessTokensAfter, accessTokensBefore + 1);

        target().path(API_VERSION).path("admin").path("oauth2").path("token")
                .path("clean")
                .request()
                .header(Attributes.AUTHORIZATION, adminAuthHeader)
                .post(null);

        assertEquals(0, accessDao.retrieveInvalidAccessTokens().size());
    }

    @Test
    public void testUpdateClientPrivilege () throws KustvaktException {
        // register a client
        Response response = registerConfidentialClient(username);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();

        // request an access token
        String clientAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(clientId, clientSecret);
        String code = requestAuthorizationCode(clientId, userAuthHeader);
        node = requestTokenWithAuthorizationCodeAndHeader(clientId, code,
                clientAuthHeader);
        String accessToken = node.at("/access_token").asText();

        //update client priviledge to super client
        Form form = new Form();
        form.param("client_id", clientId);
        form.param("super", Boolean.toString(true));
        
        testUpdateClientPriviledgeUnauthorized(form);
        
        response = updateClientPrivilege("admin", form);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        testAccessTokenAfterUpgradingClient(clientId, accessToken);

        // degrade a super client to a common client
        
        updateClientPrivilegeWithAdminToken(clientId);
        
        testAccessTokenAfterDegradingSuperClient(clientId, accessToken);

        deregisterClient(username, clientId);
    }

    // old access tokens retain their scopes
    private void testAccessTokenAfterUpgradingClient (String clientId,
            String accessToken) throws KustvaktException {

        JsonNode node = retrieveClientInfo(clientId, "admin");
        assertTrue(node.at("/super").asBoolean());

        // list vc
        Response response = target().path(API_VERSION).path("vc")
                .request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Scope vc_info is not authorized",
                node.at("/errors/0/1").asText());

        // search
        response = searchWithAccessToken(accessToken);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testAccessTokenAfterDegradingSuperClient (String clientId,
            String accessToken) throws KustvaktException {
        JsonNode node = retrieveClientInfo(clientId, username);
        assertTrue(node.at("/isSuper").isMissingNode());

        Response response = searchWithAccessToken(accessToken);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        String entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is invalid",
                node.at("/errors/0/1").asText());
    }
}
