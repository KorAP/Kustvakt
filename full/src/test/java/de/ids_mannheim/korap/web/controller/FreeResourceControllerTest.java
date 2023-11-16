package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

@ContextConfiguration("classpath:test-resource-config.xml")
public class FreeResourceControllerTest extends SpringJerseyTest {

    @Test
    public void testResource () throws KustvaktException {
        Response response = target().path(API_VERSION).path("resource")
                .request().get();
        String entity = response.readEntity(String.class);
        JsonNode n = JsonUtils.readTree(entity).get(0);
        assertEquals(n.at("/resourceId").asText(), "WPD17");
        assertEquals(n.at("/titles/de").asText(),
                "Deutsche Wikipedia Artikel 2017");
        assertEquals(n.at("/titles/en").asText(),
                "German Wikipedia Articles 2017");
        assertEquals(1, n.at("/languages").size());
        assertEquals(6, n.at("/layers").size());
    }
}
