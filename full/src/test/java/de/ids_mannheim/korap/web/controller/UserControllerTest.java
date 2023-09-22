package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

public class UserControllerTest extends OAuth2TestBase {

    private String username = "User\"ControllerTest";

    private String userAuthHeader;

    public UserControllerTest() throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler.createBasicAuthorizationHeaderValue(username, "password");
    }

    private OAuth2ClientJson createOAuth2Client() {
        OAuth2ClientJson client = new OAuth2ClientJson();
        client.setName("OWID client");
        client.setType(OAuth2ClientType.PUBLIC);
        client.setDescription("OWID web-based client");
        client.setRedirectURI("https://www.owid.de");
        return client;
    }

    private String registerClient() throws ProcessingException, KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2Client();
        Response response = registerClient(username, clientJson);
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String clientId = node.at("/client_id").asText();
        return clientId;
    }

    private String requestOAuth2AccessToken(String clientId) throws KustvaktException {
        Response response = requestAuthorizationCode("code", clientId, "", "user_info", "", userAuthHeader);
        String code = parseAuthorizationCode(response);
        response = requestTokenWithAuthorizationCodeAndForm(clientId, null, code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        String accessToken = node.at("/access_token").asText();
        return accessToken;
    }

    @Test
    public void getUsername() throws ProcessingException, KustvaktException {
        String clientId = registerClient();
        String accessToken = requestOAuth2AccessToken(clientId);
        Response response = target().path(API_VERSION).path("user").path("info").request().header(Attributes.AUTHORIZATION, "Bearer " + accessToken).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(username, node.at("/username").asText());
        deregisterClient(username, clientId);
    }
}
