package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

public class MatchInfoServiceTest extends FastJerseyTest {

    @BeforeClass
    public static void configure () throws Exception {
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }


    @Test
    public void testGetMatchInfoPublicCorpus () {

        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("GOE").path("AGI").path("04846")
                .path("p36875-36876").path("matchInfo")
                .queryParam("foundry", "*")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals("GOE/AGI/04846", node.at("/textSigle").asText());
        assertEquals("Zweiter römischer Aufenthalt",
                node.at("/title").asText());
        assertEquals("vom Juni 1787 bis April 1788",
                node.at("/subTitle").asText());
        assertEquals("Goethe, Johann Wolfgang von",
                node.at("/author").asText());
        assertTrue(node.at("/snippet").asText()
                .startsWith("<span class=\"context-left\"></span>"
                        + "<span class=\"match\"><span title=\"corenlp/p:ADV\">"
                        + "<span title=\"opennlp/p:ADV\">"
                        + "<span title=\"tt/l:fern\">"
                        ));
    }

    @Test
    public void testGetMatchInfoWithAuthentication () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("WPD15").path("B07").path("51608")
                .path("p46-57").path("matchInfo")
                .queryParam("foundry", "*")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals("WPD15/B07/51608", node.at("/textSigle").asText());
        assertEquals("Betty Allen",
                node.at("/title").asText());
        assertEquals("Monsieurbecker, u.a.",
                node.at("/author").asText());
        assertTrue(!node.at("/snippet").asText().isEmpty());
    }
//    @Test
//    public void testMatchInfoSave () {
//
//    }
//
//
//    @Test
//    public void testMatchInfoDelete () {
//
//    }
//
//
//    @Test
//    public void testGetMatches () {
//
//    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().runBootInterfaces();
    }
}