package de.ids_mannheim.korap.authentication;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class AuthenticationFilterTest extends SpringJerseyTest {

    @Test
    public void testAuthenticationWithUnknownScheme ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .request()
                .header(Attributes.AUTHORIZATION, "Blah blah")
                .get();

        String entity = response.readEntity(String.class);
        JsonNode n = JsonUtils.readTree(entity);

        assertEquals("2001", n.at("/errors/0/0").asText());
        assertEquals("Authentication scheme is not supported.",
                n.at("/errors/0/1").asText());
        assertEquals("Blah", n.at("/errors/0/2").asText());
    }
}
