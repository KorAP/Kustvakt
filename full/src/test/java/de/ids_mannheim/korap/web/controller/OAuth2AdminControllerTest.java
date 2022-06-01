package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;

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

    private ClientResponse updateClientPrivilege (String username,
            MultivaluedMap<String, String> form)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("admin").path("client").path("privilege")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        return response;
    }
    
    private void updateClientPriviledge (String clientId, boolean isSuper)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);
        form.add("super", Boolean.toString(isSuper));

        ClientResponse response = updateClientPrivilege(username, form);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
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

        resource().path(API_VERSION).path("oauth2").path("admin").path("token")
                .path("clean").header(Attributes.AUTHORIZATION, adminAuthHeader)
                .get(ClientResponse.class);

        assertEquals(0, refreshDao.retrieveInvalidRefreshTokens().size());
        assertEquals(0, accessDao.retrieveInvalidAccessTokens().size());
    }

    @Test
    public void testCleanRevokedTokens () throws KustvaktException {

        int accessTokensBefore = accessDao.retrieveInvalidAccessTokens().size();
        String code = requestAuthorizationCode(publicClientId, userAuthHeader);

        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, clientSecret, code);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        String accessToken = node.at("/access_token").asText();
        testRevokeToken(accessToken, publicClientId, null, ACCESS_TOKEN_TYPE);

        int accessTokensAfter = accessDao.retrieveInvalidAccessTokens().size();
        assertEquals(accessTokensAfter, accessTokensBefore + 1);

        resource().path(API_VERSION).path("oauth2").path("admin").path("token")
                .path("clean").header(Attributes.AUTHORIZATION, adminAuthHeader)
                .get(ClientResponse.class);

        assertEquals(0, accessDao.retrieveInvalidAccessTokens().size());
    }

    @Test
    public void testUpdateClientPrivilege () throws KustvaktException {
        // register a client
        ClientResponse response = registerConfidentialClient(username);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
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
        ClientResponse response = resource().path(API_VERSION).path("vc")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
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

        ClientResponse response = searchWithAccessToken(accessToken);
        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        String entity = response.getEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is invalid",
                node.at("/errors/0/1").asText());
    }
}
