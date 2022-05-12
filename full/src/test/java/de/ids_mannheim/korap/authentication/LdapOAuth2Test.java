package de.ids_mannheim.korap.authentication;

import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;

import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.unboundid.ldap.sdk.LDAPException;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.AuthenticationMethod;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.dao.AccessTokenDao;
import de.ids_mannheim.korap.oauth2.dao.OAuth2ClientDao;
import de.ids_mannheim.korap.oauth2.entity.AccessToken;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.controller.OAuth2TestBase;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

public class LdapOAuth2Test extends OAuth2TestBase {

    @Autowired
    private FullConfiguration config;
    @Autowired
    private AccessTokenDao accessDao;
    @Autowired
    private OAuth2ClientDao clientDao;
    
    private String testUserEmail = "testuser@example.com";

    @BeforeClass
    public static void startTestLDAPServer ()
            throws LDAPException, GeneralSecurityException {
        LdapAuth3Test.startDirectoryServer();

    }

    @AfterClass
    public static void stopTestLDAPServer () {
        LdapAuth3Test.shutDownDirectoryServer();
    }

    @Before
    public void setLDAPAuthentication () {
        config.setOAuth2passwordAuthentication(AuthenticationMethod.LDAP);
    }

    @After
    public void resetAuthenticationMethod () {
        config.setOAuth2passwordAuthentication(AuthenticationMethod.TEST);
    }

    @Test
    public void testRequestTokenPasswordUnknownUser ()
            throws KustvaktException {

        ClientResponse response = requestTokenWithPassword(superClientId,
                clientSecret, "unknown", "password");

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(2023, node.at("/errors/0/0").asInt());
        assertEquals(
                "LDAP Authentication failed due to unknown user or password!",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testMapUsernameToEmail () throws KustvaktException {
        ClientResponse response = requestTokenWithPassword(superClientId,
                clientSecret, "testUser", "password");
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String accessToken = node.at("/access_token").asText();
        AccessToken accessTokenObj = accessDao.retrieveAccessToken(accessToken);
        assertEquals(testUserEmail, accessTokenObj.getUserId());

        testRegisterClient(accessToken);
    }

    private void testRegisterClient (String accessToken)
            throws ClientHandlerException, UniformInterfaceException,
            KustvaktException {
        OAuth2ClientJson json = new OAuth2ClientJson();
        json.setName("LDAP test client");
        json.setType(OAuth2ClientType.PUBLIC);
        json.setDescription(
                "Test registering a public client with LDAP authentication");

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("register")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String clientId = node.at("/client_id").asText();
        OAuth2Client client = clientDao.retrieveClientById(clientId);
        assertEquals(testUserEmail, client.getRegisteredBy());
    }
}
