package de.ids_mannheim.korap.authentication;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.AuthenticationMethod;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.controller.OAuth2TestBase;

public class LdapTest extends OAuth2TestBase {

    @Autowired
    private FullConfiguration config;
    
    @Test
    public void testRequestTokenPasswordUnknownUser ()
            throws KustvaktException {
        
        config.setOAuth2passwordAuthentication(AuthenticationMethod.LDAP);
        ClientResponse response = requestTokenWithPassword(superClientId,
                clientSecret, "unknown", "password");

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2022, node.at("/errors/0/0").asInt());
        assertEquals(
                "LDAP Authentication failed due to unknown user or password!",
                node.at("/errors/0/1").asText());
        config.setOAuth2passwordAuthentication(AuthenticationMethod.TEST);
    }
}
