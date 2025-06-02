package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.SearchKrill;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;

/**
 * @author margaretha
 */
public class IndexControllerTest extends SpringJerseyTest {

    @Autowired
    private SearchKrill searchKrill;

    @Test
    public void testRecachingVC_AfterClosingIndex () throws IOException, KustvaktException,
            URISyntaxException, InterruptedException {
    	// check VC not found
    	Response response = target().path(API_VERSION).path("vc").path("~system")
                .path("named-vc1").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .get();

    	assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    	
        URI uri = IndexControllerTest.class.getClassLoader()
                .getResource("vc/named-vc1.jsonld").toURI();
        Path vcLink = Paths.get(uri);
        Path targetLink = Paths.get("vc/named-vc1.jsonld");
        Path vcPath = Paths.get("vc");
        if (!Files.exists(vcPath)) {
            Files.createDirectory(vcPath);
        }
        if (Files.exists(targetLink, LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(targetLink);
        }
        Files.copy(vcLink,targetLink);
        searchKrill.getStatistics(null);
        assertEquals(true, searchKrill.getIndex().isReaderOpen());
        Form form = new Form();
        form.param("token", "secret");
        response = target().path(API_VERSION).path("index")
                .path("close").request().post(Entity.form(form));
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(false, searchKrill.getIndex().isReaderOpen());
        // Cleaning database and cache
        Thread.sleep(200);
        
        // check VC has been cached after closing index
        response = target().path(API_VERSION).path("vc").path("~system")
                .path("named-vc1").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .get();

    	assertEquals(HttpStatus.SC_OK, response.getStatus());
    	String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        // clean up
        response = target().path(API_VERSION).path("vc").path("~system")
                .path("named-vc1").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .delete();
        response = target().path(API_VERSION).path("vc").path("~system")
                .path("named-vc1").request().get();
        entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
    }
}
