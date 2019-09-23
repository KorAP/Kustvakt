package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

/** 
 * @author margaretha, diewald
 *
 */
public class StatisticsControllerTest extends SpringJerseyTest {

    @Test
    public void testGetStatisticsNoResource ()
            throws IOException, KustvaktException {
        String corpusQuery = "corpusSigle=WPD15";
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics")
                .queryParam("corpusQuery", corpusQuery)
                .get(ClientResponse.class);

        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.get("documents").asInt(),0);
        assertEquals(node.get("tokens").asInt(),0);
    }

    @Test
    public void testStatisticsWithCq () throws KustvaktException{
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics")
                .queryParam("cq", "textType=Abhandlung & corpusSigle=GOE")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertEquals(2, node.at("/documents").asInt());
        assertEquals(138180, node.at("/tokens").asInt());
        assertEquals(5687, node.at("/sentences").asInt());
        assertEquals(258, node.at("/paragraphs").asInt());
        
        assertTrue(node.at("/warnings").isMissingNode());
    }
    
    @Test
    public void testStatisticsWithCqAndCorpusQuery () throws KustvaktException{
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics")
                .queryParam("cq", "textType=Abhandlung & corpusSigle=GOE")
                .queryParam("corpusQuery", "textType=Autobiographie & corpusSigle=GOE")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertEquals(2, node.at("/documents").asInt());
        assertEquals(138180, node.at("/tokens").asInt());
        assertEquals(5687, node.at("/sentences").asInt());
        assertEquals(258, node.at("/paragraphs").asInt());
        
        assertTrue(node.at("/warnings").isMissingNode());
    }

    @Test
    public void testGetStatisticsWithcorpusQuery1 ()
            throws IOException, KustvaktException {
        String corpusQuery = "corpusSigle=GOE";
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics")
                .queryParam("corpusQuery", corpusQuery)
                .get(ClientResponse.class);

        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.get("documents").asInt(),11);
        assertEquals(node.get("tokens").asInt(),665842);
        
        assertEquals(StatusCodes.DEPRECATED_PARAMETER,
                node.at("/warnings/0/0").asInt());
        assertEquals("Parameter corpusQuery is deprecated in favor of cq.",
                node.at("/warnings/0/1").asText());
    }


    @Test
    public void testGetStatisticsWithcorpusQuery2 ()
            throws IOException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics")
                .queryParam("corpusQuery", "creationDate since 1810")
                .get(ClientResponse.class);
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
        assertEquals(node.get("documents").asInt(),7);
        assertEquals(node.get("tokens").asInt(),279402);
        assertEquals(node.get("sentences").asInt(), 11047);
        assertEquals(node.get("paragraphs").asInt(), 489);
    }


    @Test
    public void testGetStatisticsWithWrongcorpusQuery ()
            throws IOException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics")
                .queryParam("corpusQuery", "creationDate geq 1810")
                .get(ClientResponse.class);

        assert ClientResponse.Status.BAD_REQUEST.getStatusCode() == response
                .getStatus();
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/errors/0/0").asInt(), 302);
        assertEquals(node.at("/errors/0/1").asText(),
                "Could not parse query >>> (creationDate geq 1810) <<<.");
        assertEquals(node.at("/errors/0/2").asText(),
                "(creationDate geq 1810)");
    }


    @Test
    public void testGetStatisticsWithWrongcorpusQuery2 ()
            throws IOException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics")
                .queryParam("corpusQuery", "creationDate >= 1810")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/errors/0/0").asInt(), 305);
        assertEquals(node.at("/errors/0/1").asText(),
                "Operator >= is not acceptable.");
        assertEquals(node.at("/errors/0/2").asText(), ">=");
    }

    
    @Test
    public void testGetStatisticsWithoutcorpusQuery ()
            throws IOException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
					 response.getStatus());
        String ent = response.getEntity(String.class);

		JsonNode node = JsonUtils.readTree(ent);
		assertEquals(11, node.at("/documents").asInt());
        assertEquals(665842, node.at("/tokens").asInt());
        assertEquals(25074, node.at("/sentences").asInt());
        assertEquals(772, node.at("/paragraphs").asInt());
    }
   
    @Test
    public void testGetStatisticsWithKoralQuery ()
            throws IOException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .post(ClientResponse.class,"{ \"collection\" : {\"@type\": "
                        + "\"koral:doc\", \"key\": \"availability\", \"match\": "
                        + "\"match:eq\", \"type\": \"type:regex\", \"value\": "
                        + "\"CC-BY.*\"} }");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                     response.getStatus());
        String ent = response.getEntity(String.class);
        
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(2, node.at("/documents").asInt());
        assertEquals(72770, node.at("/tokens").asInt());
        assertEquals(2985, node.at("/sentences").asInt());
        assertEquals(128, node.at("/paragraphs").asInt());
    }
    
    @Test
    public void testGetStatisticsWithEmptyCollection ()
            throws IOException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .post(ClientResponse.class,"{}");

        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                     response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/errors/0/0").asInt(),
                de.ids_mannheim.korap.util.StatusCodes.MISSING_COLLECTION);
        assertEquals(node.at("/errors/0/1").asText(),
                "Collection is not found");
    }
    
    @Test
    public void testGetStatisticsWithIncorrectJson ()
            throws IOException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .post(ClientResponse.class,"{ \"collection\" : }");

        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                     response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/errors/0/0").asInt(),
                de.ids_mannheim.korap.util.StatusCodes.UNABLE_TO_PARSE_JSON);
        assertEquals(node.at("/errors/0/1").asText(),
                "Unable to parse JSON");
    }
    
    @Test
    public void testGetStatisticsWithoutKoralQuery ()
            throws IOException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics").post(ClientResponse.class);

        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.NO_QUERY, node.at("/errors/0/0").asInt());
        assertEquals("Koral query is missing",
                node.at("/errors/0/1").asText());
        assertEquals("koralQuery", node.at("/errors/0/2").asText());
    }
    
}
