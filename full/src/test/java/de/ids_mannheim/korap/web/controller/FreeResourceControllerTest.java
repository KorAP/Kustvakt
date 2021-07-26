package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

@ContextConfiguration("classpath:test-resource-config.xml")
public class FreeResourceControllerTest extends SpringJerseyTest {
    
    @Test
    public void testResource () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION)
                .path("resource").get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode n = JsonUtils.readTree(entity).get(0);
        assertEquals("WPD17",n.at("/resourceId").asText());
        assertEquals("Deutsche Wikipedia Artikel 2017", n.at("/titles/de").asText());
        assertEquals("German Wikipedia Articles 2017", n.at("/titles/en").asText());
        assertEquals(1, n.at("/languages").size());
        assertEquals(6, n.at("/layers").size());
    }

}
