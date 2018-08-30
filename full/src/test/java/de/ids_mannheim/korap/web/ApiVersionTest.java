package de.ids_mannheim.korap.web;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;

public class ApiVersionTest extends SpringJerseyTest {

    @Test
    public void testSearchWithoutVersion () throws KustvaktException {
        ClientResponse response = resource().path("api").path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        assertEquals(HttpStatus.PERMANENT_REDIRECT_308, response.getStatus());
        URI location = response.getLocation();
        assertEquals("/api/v1.0/search", location.getPath());
    }

    @Test
    public void testSearchWrongVersion () throws KustvaktException {
        ClientResponse response = resource().path("api").path("v0.2")
                .path("search").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").accept(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
        assertEquals(HttpStatus.PERMANENT_REDIRECT_308, response.getStatus());
        URI location = response.getLocation();
        assertEquals("/api/v1.0/search", location.getPath());
    }
}
