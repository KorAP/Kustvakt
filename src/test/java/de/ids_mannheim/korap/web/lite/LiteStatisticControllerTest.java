package de.ids_mannheim.korap.web.lite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.LiteJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class LiteStatisticControllerTest extends LiteJerseyTest {

    @Test
    public void testStatisticsWithCq () throws KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", "textType=Abhandlung & corpusSigle=GOE")
                .request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(response.getHeaders().getFirst("X-Index-Revision"),
                "Wes8Bd4h1OypPqbWF5njeQ==");
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertEquals(2, node.at("/documents").asInt());
        assertEquals(138180, node.at("/tokens").asInt());
        assertEquals(5687, node.at("/sentences").asInt());
        assertEquals(258, node.at("/paragraphs").asInt());
        assertTrue(node.at("/warnings").isMissingNode());
    }

    @Test
    public void testStatisticsEmptyCq () throws KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", "").request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertEquals(11, node.at("/documents").asInt());
        assertEquals(665842, node.at("/tokens").asInt());
        assertEquals(25074, node.at("/sentences").asInt());
        assertEquals(772, node.at("/paragraphs").asInt());
        response = target().path(API_VERSION).path("statistics").request()
                .method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        query = response.readEntity(String.class);
        node = JsonUtils.readTree(query);
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
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        assertEquals(response.getHeaders().getFirst("X-Index-Revision"),
                "Wes8Bd4h1OypPqbWF5njeQ==");
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
        assertEquals(node.at("/errors/0/0").asInt(),
                de.ids_mannheim.korap.util.StatusCodes.MISSING_COLLECTION);
        assertEquals(node.at("/errors/0/1").asText(),
                "Collection is not found");
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
        assertEquals(node.at("/errors/0/1").asText(),
                "Failed deserializing json object: { \"collection\" : }");
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
