package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.SpringJerseyTest;

/** 
 * @author margaretha, diewald
 * @date 27/09/2017
 *
 */
public class StatisticsControllerTest extends SpringJerseyTest {

    private ObjectMapper mapper = new ObjectMapper();


    @Test
    public void testGetStatisticsNoResource ()
            throws JsonProcessingException, IOException {
        String corpusQuery = "corpusSigle=WPD15";
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("corpusQuery", corpusQuery)
                .get(ClientResponse.class);

        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();

        String ent = response.getEntity(String.class);
        JsonNode node = mapper.readTree(ent);
        assertEquals(node.get("documents").asInt(),0);
        assertEquals(node.get("tokens").asInt(),0);
    }


    @Test
    public void testGetStatisticsWithcorpusQuery1 ()
            throws JsonProcessingException, IOException {
        String corpusQuery = "corpusSigle=GOE";
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("corpusQuery", corpusQuery)
                .get(ClientResponse.class);

        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();

        String ent = response.getEntity(String.class);
        JsonNode node = mapper.readTree(ent);
        assertEquals(node.get("documents").asInt(),11);
        assertEquals(node.get("tokens").asInt(),665842);
    }


    @Test
    public void testGetStatisticsWithcorpusQuery2 ()
            throws JsonProcessingException, IOException {
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("corpusQuery", "creationDate since 1810")
                .get(ClientResponse.class);
        String ent = response.getEntity(String.class);
        JsonNode node = mapper.readTree(ent);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
        assertEquals(node.get("documents").asInt(),7);
        assertEquals(node.get("tokens").asInt(),279402);
        assertEquals(node.get("sentences").asInt(), 11047);
        assertEquals(node.get("paragraphs").asInt(), 489);
    }


    @Test
    public void testGetStatisticsWithWrongcorpusQuery ()
            throws JsonProcessingException, IOException {
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("corpusQuery", "creationDate geq 1810")
                .get(ClientResponse.class);

        assert ClientResponse.Status.BAD_REQUEST.getStatusCode() == response
                .getStatus();
        String ent = response.getEntity(String.class);
        JsonNode node = mapper.readTree(ent);
        assertEquals(node.at("/errors/0/0").asInt(), 302);
        assertEquals(node.at("/errors/0/1").asText(),
                "Could not parse query >>> (creationDate geq 1810) <<<.");
        assertEquals(node.at("/errors/0/2").asText(),
                "(creationDate geq 1810)");
    }


    @Test
    public void testGetStatisticsWithWrongcorpusQuery2 ()
            throws JsonProcessingException, IOException {
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("corpusQuery", "creationDate >= 1810")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = mapper.readTree(ent);
        assertEquals(node.at("/errors/0/0").asInt(), 305);
        assertEquals(node.at("/errors/0/1").asText(),
                "Operator >= is not acceptable.");
        assertEquals(node.at("/errors/0/2").asText(), ">=");
    }

    
    @Test
    public void testGetStatisticsWithoutcorpusQuery ()
            throws JsonProcessingException, IOException {
        ClientResponse response = resource()
                .path("statistics")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
					 response.getStatus());
        String ent = response.getEntity(String.class);

		JsonNode node = mapper.readTree(ent);
		assertEquals(11, node.at("/documents").asInt());
        assertEquals(665842, node.at("/tokens").asInt());
        assertEquals(25074, node.at("/sentences").asInt());
        assertEquals(772, node.at("/paragraphs").asInt());
    }
   
}
