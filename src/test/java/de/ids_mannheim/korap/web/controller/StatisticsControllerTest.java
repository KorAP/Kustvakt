package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha, diewald
 */
public class StatisticsControllerTest extends SpringJerseyTest {

    @Test
    public void testGetStatisticsNoResource ()
            throws IOException, KustvaktException {
        String corpusQuery = "corpusSigle=WPD15";
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", corpusQuery).request().get();
        assert Status.OK.getStatusCode() == response.getStatus();
        assertEquals(response.getHeaders().getFirst("X-Index-Revision"),
                "Wes8Bd4h1OypPqbWF5njeQ==");
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(0,node.get("documents").asInt());
        assertEquals(0,node.get("tokens").asInt());
    }

    @Test
    public void testStatisticsWithCq () throws KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", "textType=Abhandlung & corpusSigle=GOE")
                .request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String query = response.readEntity(String.class);
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
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", corpusQuery).request().get();
        assert Status.OK.getStatusCode() == response.getStatus();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(11,node.get("documents").asInt());
        assertEquals(665842,node.get("tokens").asInt());
    }

    @Test
    public void testGetStatisticsWithcorpusQuery2 ()
            throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", "creationDate since 1810").request()
                .get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assert Status.OK.getStatusCode() == response.getStatus();
        assertEquals(7,node.get("documents").asInt());
        assertEquals(279402,node.get("tokens").asInt());
        assertEquals(11047,node.get("sentences").asInt());
        assertEquals(489,node.get("paragraphs").asInt());
    }

    @Test
    public void testGetStatisticsWithWrongcorpusQuery ()
            throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", "creationDate geq 1810").request().get();
        assert Status.BAD_REQUEST.getStatusCode() == response.getStatus();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(302, node.at("/errors/0/0").asInt());
        assertEquals("Could not parse query >>> (creationDate geq 1810) <<<.",
                node.at("/errors/0/1").asText());
        assertEquals("(creationDate geq 1810)",
                node.at("/errors/0/2").asText());
    }

    @Test
    public void testGetStatisticsWithWrongcorpusQuery2 ()
            throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", "creationDate >= 1810").request().get();
        String ent = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(305, node.at("/errors/0/0").asInt());
        assertEquals("Operator >= is not acceptable.",
                node.at("/errors/0/1").asText());
        assertEquals(">=", node.at("/errors/0/2").asText());
    }

    @Test
    public void testGetStatisticsWithoutcorpusQuery ()
            throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(11, node.at("/documents").asInt());
        assertEquals(665842, node.at("/tokens").asInt());
        assertEquals(25074, node.at("/sentences").asInt());
        assertEquals(772, node.at("/paragraphs").asInt());
    }

    @Test
    public void testGetStatisticsWithKoralQuery ()
            throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .request()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .post(Entity.json("{ \"collection\" : {\"@type\": "
                        + "\"koral:doc\", \"key\": \"availability\", \"match\": "
                        + "\"match:eq\", \"type\": \"type:regex\", \"value\": "
                        + "\"CC.*\"} }"));
        assertEquals(response.getHeaders().getFirst("X-Index-Revision"),
                "Wes8Bd4h1OypPqbWF5njeQ==");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(2, node.at("/documents").asInt());
        assertEquals(72770, node.at("/tokens").asInt());
        assertEquals(2985, node.at("/sentences").asInt());
        assertEquals(128, node.at("/paragraphs").asInt());
    }

    @Test
    public void testGetStatisticsWithEmptyCollection ()
            throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .request()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .post(Entity.json("{}"));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(de.ids_mannheim.korap.util.StatusCodes.MISSING_COLLECTION,
                node.at("/errors/0/0").asInt());
        assertEquals("Collection is not found",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testGetStatisticsWithIncorrectJson ()
            throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .request()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .post(Entity.json("{ \"collection\" : }"));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Failed deserializing json object: { \"collection\" : }",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testGetStatisticsWithoutKoralQuery ()
            throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .request().post(Entity.json(""));
        String ent = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(11, node.at("/documents").asInt());
        assertEquals(665842, node.at("/tokens").asInt());
        assertEquals(25074, node.at("/sentences").asInt());
        assertEquals(772, node.at("/paragraphs").asInt());
    }
    
    @Test
    public void testStatisticsWithNamedVC () throws KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", "referTo unknownVC")
                .request().method("GET");
        String ent = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(0, node.at("/documents").asInt());
        assertEquals(0, node.at("/tokens").asInt());
        assertEquals(0, node.at("/sentences").asInt());
        assertEquals(0, node.at("/paragraphs").asInt());
    }
}
