package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.jetty.http.HttpHeaders;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.authentication.BasicHttpAuth;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

public class MatchInfoServiceTest extends FastJerseyTest {

    @Test
    public void testGetMatchInfoPublicCorpus () throws KustvaktException {

        ClientResponse response = resource()
                .path("corpus").path("GOE").path("AGA").path("01784")
                .path("p36-100").path("matchInfo")
                .queryParam("foundry", "*")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertNotNull(node);
        assertEquals("GOE/AGA/01784", node.at("/textSigle").asText());
        assertEquals("Belagerung von Mainz",
                node.at("/title").asText());
        assertEquals("Goethe, Johann Wolfgang von",
                node.at("/author").asText());
        assertTrue(node.at("/snippet").asText()
                .startsWith("<span class=\"context-left\"></span>"
                        + "<span class=\"match\">"
					));
    }
    
    @Test
    public void testGetMatchInfoNotAllowed () throws KustvaktException {

        ClientResponse response = resource()
                .path("corpus").path("GOE").path("AGI").path("04846")
                .path("p36875-36876").path("matchInfo")
                .queryParam("foundry", "*")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals("1003", node.at("/errors/0/0").asText());
        assertEquals("Retrieving match info with ID "
        		+ "match-GOE/AGI/04846-p36875-36876 is not allowed.", 
        		node.at("/errors/0/1").asText());
        assertTrue(node.at("/snippet").isMissingNode());
    }

    @Test
    public void testGetMatchInfoWithAuthentication () throws KustvaktException {
        ClientResponse response = resource()
                .path("corpus").path("GOE").path("AGI").path("04846")
                .path("p36875-36876").path("matchInfo")
                .queryParam("foundry", "*")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "172.27.0.32")
                .get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        
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
                        + "<span class=\"match\">"
					));
        assertEquals("QAO-NC-LOC:ids", node.at("/availability").asText());
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
//        helper().runBootInterfaces();
    }
}
