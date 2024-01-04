package de.ids_mannheim.korap.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;

public class AuthenticationFilterTest extends SpringJerseyTest {

    @Test
    public void testAuthenticationWithUnknownScheme ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .request().header(Attributes.AUTHORIZATION, "Blah blah").get();
        String entity = response.readEntity(String.class);
        JsonNode n = JsonUtils.readTree(entity);
        assertEquals(n.at("/errors/0/0").asText(), "2001");
        assertEquals(n.at("/errors/0/1").asText(),
                "Authentication scheme is not supported.");
        assertEquals(n.at("/errors/0/2").asText(), "Blah");
    }
}
