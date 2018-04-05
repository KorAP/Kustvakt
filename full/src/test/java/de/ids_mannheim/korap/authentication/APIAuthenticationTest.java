package de.ids_mannheim.korap.authentication;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;

public class APIAuthenticationTest extends SpringJerseyTest {

    @Autowired
    private KustvaktConfiguration config;

    @Test
    public void testCreateGetTokenContext ()
            throws KustvaktException, IOException, InterruptedException {
        User user = new KorAPUser();
        user.setUsername("testUser");

        Map<String, Object> attr = new HashMap<>();
        attr.put(Attributes.HOST, "localhost");
        attr.put(Attributes.USER_AGENT, "java");

        APIAuthentication auth = new APIAuthentication(config);
        TokenContext context = auth.createTokenContext(user, attr);

        // get token context
        String authToken = context.getToken();
        context = auth.getTokenContext(authToken);

        TokenType tokenType = context.getTokenType();
        assertEquals(TokenType.API, tokenType);
        assertEquals("testUser", context.getUsername());
    }

}
