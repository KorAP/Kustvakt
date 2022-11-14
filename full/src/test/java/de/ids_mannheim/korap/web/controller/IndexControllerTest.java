package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.SearchKrill;

/**
 * @author margaretha
 *
 */
public class IndexControllerTest extends SpringJerseyTest {

    @Autowired
    private SearchKrill searchKrill;

    @Test
    public void testCloseIndex () throws IOException, KustvaktException,
            URISyntaxException, InterruptedException {
        URI uri = IndexControllerTest.class.getClassLoader()
                .getResource("vc/named-vc1.jsonld").toURI();
        Path toLink = Paths.get(uri);
        Path symLink = Paths.get("vc/named-vc1.jsonld");
        Path vcPath = Paths.get("vc");
        if (!Files.exists(vcPath)) {
            Files.createDirectory(vcPath);
        }
        if (Files.exists(symLink, LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(symLink);
        }
        Files.createSymbolicLink(symLink, toLink);

        searchKrill.getStatistics(null);
        assertEquals(true, searchKrill.getIndex().isReaderOpen());

        Form form = new Form();
        form.param("token", "secret");

        Response response = target().path(API_VERSION).path("index")
                .path("close")
                .request()
                .post(Entity.form(form));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(false, searchKrill.getIndex().isReaderOpen());

        
        // Cleaning database and cache
        
        Thread.sleep(200);

        response = target().path(API_VERSION).path("vc").path("~system")
                .path("named-vc1")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("admin", "pass"))
                .delete();

        response = target().path(API_VERSION).path("vc").path("~system")
                .path("named-vc1")
                .request()
                .get();
        
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,node.at("/errors/0/0").asInt());
    }

}
