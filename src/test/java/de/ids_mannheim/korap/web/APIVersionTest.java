package de.ids_mannheim.korap.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.Set;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author margaretha
 */
public class APIVersionTest extends SpringJerseyTest {

    @Autowired
    private KustvaktConfiguration config;
    
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
    
    @Test
    public void testSupportedVersions () {
        Set<String> versions = config.getSupportedVersions();
        assertEquals(2, versions.size());
        
        String version = versions.stream().findFirst().orElse("");
        assertEquals(4, version.length());
    }
}
