package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VirtualCorpusClientTest extends VirtualCorpusTestBase {

    private String username = "VirtualCorpusClientTest";

    @Test
    public void testVC_withClient () throws KustvaktException {
        // create client
        Response response = registerConfidentialClient(username);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();
        // obtain authorization
        String userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, "password");
        response = requestAuthorizationCode("code", clientId, clientRedirectUri,
                "create_vc vc_info delete_vc create_vc", "myState",
                userAuthHeader);
        String code = parseAuthorizationCode(response);
        response = requestTokenWithAuthorizationCodeAndForm(clientId,
                clientSecret, code, clientRedirectUri);
        node = JsonUtils.readTree(response.readEntity(String.class));
        String accessToken = node.at("/access_token").asText();
        String accessTokenHeader = "Bearer " + accessToken;
        // create VC 1
        String vcName = "vc-client1";
        String vcJson = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate since 1820\"}";
        createVC(accessTokenHeader, username, vcName, vcJson);
        // create VC 2
        vcName = "vc-client2";
        vcJson = "{\"type\": \"PRIVATE\"" + ",\"queryType\": \"VIRTUAL_CORPUS\""
                + ",\"corpusQuery\": \"creationDate until 1820\"}";
        createVC(accessTokenHeader, username, vcName, vcJson);
        // edit VC
        String description = "vc created from client";
        vcJson = "{\"description\": \"" + description + "\"}";
        editVC(username, username, vcName, vcJson);
        // retrieve vc info
        node = retrieveVCInfo(username, username, vcName);
        assertEquals(description, node.at("/description").asText());
        // list vc
        node = listVCWithAuthHeader(accessTokenHeader);
        assertEquals(3, node.size());
        // delete vc
        deleteVC(vcName, username, username);
        // list vc
        node = listVCWithAuthHeader(accessTokenHeader);
        assertEquals(2, node.size());
        // delete client
        deregisterClient(username, clientId);
        testSearchWithRevokedAccessToken(accessToken);
        // obtain authorization from another client
        response = requestTokenWithPassword(superClientId, this.clientSecret,
                username, "pass");
        node = JsonUtils.readTree(response.readEntity(String.class));
        accessToken = node.at("/access_token").asText();
        // checking vc should still be available after client deregistration
        node = listVCWithAuthHeader("Bearer " + accessToken);
        assertEquals(2, node.size());
    }
}
