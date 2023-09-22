package de.ids_mannheim.korap.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author margaretha
 */
@DisplayName("Api Version Test")
class ApiVersionTest extends SpringJerseyTest {

    @Test
    @DisplayName("Test Search Without Version")
    void testSearchWithoutVersion() throws KustvaktException {
        Response response = target().path("api").path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(HttpStatus.PERMANENT_REDIRECT_308, response.getStatus());
        URI location = response.getLocation();
        assertEquals("/api/" + API_VERSION + "/search", location.getPath());
    }

    @Test
    @DisplayName("Test Search Wrong Version")
    void testSearchWrongVersion() throws KustvaktException {
        Response response = target().path("api").path("v0.2").path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(HttpStatus.PERMANENT_REDIRECT_308, response.getStatus());
        URI location = response.getLocation();
        assertEquals("/api/" + API_VERSION + "/search", location.getPath());
    }
}
