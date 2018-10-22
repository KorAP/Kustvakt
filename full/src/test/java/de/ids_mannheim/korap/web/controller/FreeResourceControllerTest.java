package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class FreeResourceControllerTest extends SpringJerseyTest {
    
    @Test
    public void testResource () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION)
                .path("resource").path("info").get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode n = JsonUtils.readTree(entity).get(0);
        
        assertEquals("WPD15",n.at("/resourceId").asText());
        assertEquals("Deutsche Wikipedia Artikel 2015", n.at("/titles/de").asText());
        assertEquals("German Wikipedia Articles 2015", n.at("/titles/en").asText());
        assertEquals(1, n.at("/languages").size());
        assertEquals(5, n.at("/layers").size());
    }

}
