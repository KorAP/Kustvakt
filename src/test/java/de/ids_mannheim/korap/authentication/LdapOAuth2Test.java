package de.ids_mannheim.korap.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.security.GeneralSecurityException;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.unboundid.ldap.sdk.LDAPException;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.AuthenticationMethod;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.dao.OAuth2ClientDao;
import de.ids_mannheim.korap.oauth2.dao.RefreshTokenDao;
import de.ids_mannheim.korap.oauth2.entity.AccessToken;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.controller.OAuth2TestBase;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class LdapOAuth2Test extends OAuth2TestBase {

    @Autowired
    private FullConfiguration config;

    @Autowired
    private AccessTokenDao accessDao;

    @Autowired
    private RefreshTokenDao refreshDao;

    @Autowired
    private OAuth2ClientDao clientDao;

    private String testUsername = "idsTestUser";

    private String testUserEmail = "testuser@example.com";

    private String redirectUri = "https://client.com/redirect";

    @BeforeAll
    static void startTestLDAPServer ()
            throws LDAPException, GeneralSecurityException {
        LdapAuth3Test.startDirectoryServer();
    }

    @AfterAll
    static void stopTestLDAPServer () {
        LdapAuth3Test.shutDownDirectoryServer();
    }

    @BeforeEach
    public void setLDAPAuthentication () {
        config.setOAuth2passwordAuthentication(AuthenticationMethod.LDAP);
    }

    @AfterEach
    public void resetAuthenticationMethod () {
        config.setOAuth2passwordAuthentication(AuthenticationMethod.TEST);
    }

    @Test
    public void testRequestTokenPasswordUnknownUser ()
            throws KustvaktException {
        Response response = requestTokenWithPassword(superClientId,
                clientSecret, "unknown", "password");
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2023, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(),
                "LDAP Authentication failed due to unknown user or password!");
    }

    @Test
    public void testMapEmailToUsername () throws KustvaktException {
        Response response = requestTokenWithPassword(superClientId,
                clientSecret, testUserEmail, "password");
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String accessToken = node.at("/access_token").asText();
        AccessToken accessTokenObj = accessDao.retrieveAccessToken(accessToken);
        assertEquals(testUsername, accessTokenObj.getUserId());
        String refreshToken = node.at("/refresh_token").asText();
        RefreshToken rt = refreshDao.retrieveRefreshToken(refreshToken);
        assertEquals(testUsername, rt.getUserId());
        testRegisterPublicClient(accessToken);
        node = testRegisterConfidentialClient(accessToken);
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();
        testRequestTokenWithAuthorization(clientId, clientSecret, accessToken);
    }

    private void testRegisterPublicClient (String accessToken)
            throws KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("LDAP test client");
        json.setType(OAuth2ClientType.PUBLIC);
        json.setDescription(
                "Test registering a public client with LDAP authentication");
        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path("register").request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(Entity.json(json));
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String clientId = node.at("/client_id").asText();
        OAuth2Client client = clientDao.retrieveClientById(clientId);
        assertEquals(testUsername, client.getRegisteredBy());
    }

    private JsonNode testRegisterConfidentialClient (String accessToken)
            throws KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("LDAP test client");
        json.setType(OAuth2ClientType.CONFIDENTIAL);
        json.setRedirectURI(redirectUri);
        json.setDescription(
                "Test registering a confidential client with LDAP authentication");
        Response response = target().path(API_VERSION).path("oauth2")
                .path("client").path("register").request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(Entity.json(json));
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String clientId = node.at("/client_id").asText();
        OAuth2Client client = clientDao.retrieveClientById(clientId);
        assertEquals(testUsername, client.getRegisteredBy());
        return node;
    }

    private void testRequestTokenWithAuthorization (String clientId,
            String clientSecret, String accessToken) throws KustvaktException {
        String authHeader = "Bearer " + accessToken;
        Response response = target().path(API_VERSION).path("oauth2")
                .path("authorize").queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("scope", "search match_info").request()
                .header(Attributes.AUTHORIZATION, authHeader).get();
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());
        URI redirectUri = response.getLocation();
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(redirectUri).build().getQueryParams();
        String code = params.getFirst("code");
        response = requestTokenWithAuthorizationCodeAndForm(clientId,
                clientSecret, code);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String at = node.at("/access_token").asText();
        AccessToken accessTokenObj = accessDao.retrieveAccessToken(at);
        assertEquals(testUsername, accessTokenObj.getUserId());
        String refreshToken = node.at("/refresh_token").asText();
        RefreshToken rt = refreshDao.retrieveRefreshToken(refreshToken);
        assertEquals(testUsername, rt.getUserId());
    }
}
