package de.ids_mannheim.korap.web.service.full;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/** 
 * @author margaretha
 * @date 29/06/2017
 *
 */
public class StatisticsServiceTest extends FastJerseyTest {

    private ObjectMapper mapper = new ObjectMapper();


    @Override
    public void initMethod () throws KustvaktException {

    }

    @BeforeClass
    public static void configure () {
//        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.light",
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.utils");
    }

    @Test
    public void testGetStatisticsNoResource ()
            throws JsonProcessingException, IOException {
        String collectionQuery = "corpusSigle=WPD15";
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("collectionQuery", collectionQuery)
                .get(ClientResponse.class);

        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();

        String ent = response.getEntity(String.class);
        JsonNode node = mapper.readTree(ent);
        assertEquals(node.get("documents").asInt(),0);
        assertEquals(node.get("tokens").asInt(),0);
    }


    @Test
    public void testGetStatisticsWithCollectionQuery1 ()
            throws JsonProcessingException, IOException {
        String collectionQuery = "corpusSigle=GOE";
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("collectionQuery", collectionQuery)
                .get(ClientResponse.class);

        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();

        String ent = response.getEntity(String.class);
        JsonNode node = mapper.readTree(ent);
        assertEquals(node.get("documents").asInt(),11);
        assertEquals(node.get("tokens").asInt(),665842);
    }


    @Test
    public void testGetStatisticsWithCollectionQuery2 ()
            throws JsonProcessingException, IOException {
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("collectionQuery", "creationDate since 1810")
                .get(ClientResponse.class);
        String ent = response.getEntity(String.class);
        JsonNode node = mapper.readTree(ent);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
        assertEquals(node.get("documents").asInt(),7);
        assertEquals(node.get("tokens").asInt(),279402);
        // EM: why zero?
        assertEquals(node.get("sentences").asInt(), 11047);
        assertEquals(node.get("paragraphs").asInt(), 489);
    }


    @Test
    public void testGetStatisticsWithWrongCollectionQuery ()
            throws JsonProcessingException, IOException {
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("collectionQuery", "creationDate geq 1810")
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
    public void testGetStatisticsWithWrongCollectionQuery2 ()
            throws JsonProcessingException, IOException {
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("collectionQuery", "creationDate >= 1810")
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

}
