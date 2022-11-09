package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response.Status;

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
        Response response = target().path(API_VERSION).path("oauth2")
                .path("admin").path("client").path("privilege")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));

        return response;
    }
    
    private void updateClientPriviledge (String clientId, boolean isSuper)
            throws ProcessingException,
            KustvaktException {
        Form form = new Form();
        form.param("client_id", clientId);
        form.param("super", Boolean.toString(isSuper));

        Response response = updateClientPrivilege(username, form);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());

        response = updateClientPrivilege("admin", form);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCleanExpiredTokens () {
        int refreshTokensBefore =
                refreshDao.retrieveInvalidRefreshTokens().size();
        assertTrue(refreshTokensBefore > 0);

        int accessTokensBefore = accessDao.retrieveInvalidAccessTokens().size();
        assertTrue(accessTokensBefore > 0);

        target().path(API_VERSION).path("oauth2").path("admin").path("token")
                .path("clean")
                .request()
                .header(Attributes.AUTHORIZATION, adminAuthHeader)
                .get();

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

        target().path(API_VERSION).path("oauth2").path("admin").path("token")
                .path("clean")
                .request()
                .header(Attributes.AUTHORIZATION, adminAuthHeader)
                .get();

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

        updateClientPriviledge(clientId, true);
        testAccessTokenAfterUpgradingClient(clientId, accessToken);

        updateClientPriviledge(clientId, false);
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
