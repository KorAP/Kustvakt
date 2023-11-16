package de.ids_mannheim.korap.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author margaretha
 */
public class ApiVersionTest extends SpringJerseyTest {

    @Test
    public void testSearchWithoutVersion () throws KustvaktException {
        Response response = target().path("api").path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(HttpStatus.PERMANENT_REDIRECT_308, response.getStatus());
        URI location = response.getLocation();
        assertEquals("/api/" + API_VERSION + "/search", location.getPath());
    }

    @Test
    public void testSearchWrongVersion () throws KustvaktException {
        Response response = target().path("api").path("v0.2").path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(HttpStatus.PERMANENT_REDIRECT_308, response.getStatus());
        URI location = response.getLocation();
        assertEquals("/api/" + API_VERSION + "/search", location.getPath());
    }
}
