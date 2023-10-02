package de.ids_mannheim.korap.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.nimbusds.jose.JOSEException;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.controller.OAuth2TestBase;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class APIAuthenticationTest extends OAuth2TestBase {

    @Autowired
    private FullConfiguration config;

    @Test
    public void testDeprecatedService() throws KustvaktException {
        String userAuthHeader = HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("dory", "password");
        Response response = target().path(API_VERSION).path("auth").path("apiToken").request().header(Attributes.AUTHORIZATION, userAuthHeader).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DEPRECATED, node.at("/errors/0/0").asInt());
    }

    @Test
    public void testCreateGetTokenContext() throws KustvaktException, IOException, InterruptedException, JOSEException {
        User user = new KorAPUser();
        user.setUsername("testUser");
        Map<String, Object> attr = new HashMap<>();
        attr.put(Attributes.HOST, "localhost");
        attr.put(Attributes.USER_AGENT, "java");
        attr.put(Attributes.AUTHENTICATION_TIME, TimeUtils.getNow().toDate());
        APIAuthentication auth = new APIAuthentication(config);
        TokenContext context = auth.createTokenContext(user, attr);
        // get token context
        String authToken = context.getToken();
        // System.out.println(authToken);
        context = auth.getTokenContext(authToken);
        TokenType tokenType = context.getTokenType();
        assertEquals(TokenType.API, tokenType);
        assertEquals(context.getUsername(), "testUser");
    }
}
