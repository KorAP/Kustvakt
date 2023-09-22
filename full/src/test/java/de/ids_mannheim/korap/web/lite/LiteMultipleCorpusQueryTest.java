package de.ids_mannheim.korap.web.lite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.ids_mannheim.korap.config.LiteJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Lite Multiple Corpus Query Test")
class LiteMultipleCorpusQueryTest extends LiteJerseyTest {

    @Test
    @DisplayName("Test Search Get")
    void testSearchGet() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "das").queryParam("ql", "poliqarp").queryParam("cq", "pubPlace=München").queryParam("cq", "textSigle=\"GOE/AGA/01784\"").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        node = node.at("/collection");
        assertEquals(node.at("/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/operation").asText(), "operation:and");
        assertEquals(2, node.at("/operands").size());
        assertEquals(node.at("/operands/0/@type").asText(), "koral:doc");
        assertEquals(node.at("/operands/0/match").asText(), "match:eq");
        assertEquals(node.at("/operands/0/key").asText(), "pubPlace");
        assertEquals(node.at("/operands/0/value").asText(), "München");
        assertEquals(node.at("/operands/1/key").asText(), "textSigle");
        assertEquals(node.at("/operands/1/value").asText(), "GOE/AGA/01784");
    }

    @Test
    @DisplayName("Test Statistics With Multiple Cq")
    void testStatisticsWithMultipleCq() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").queryParam("cq", "textType=Abhandlung").queryParam("cq", "corpusSigle=GOE").request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/documents").asInt());
        assertEquals(138180, node.at("/tokens").asInt());
        assertEquals(5687, node.at("/sentences").asInt());
        assertEquals(258, node.at("/paragraphs").asInt());
        assertTrue(node.at("/warnings").isMissingNode());
    }

    @Test
    @DisplayName("Test Statistics With Multiple Corpus Query")
    void testStatisticsWithMultipleCorpusQuery() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").queryParam("corpusQuery", "textType=Autobiographie").queryParam("corpusQuery", "corpusSigle=GOE").request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(9, node.at("/documents").asInt());
        assertEquals(527662, node.at("/tokens").asInt());
        assertEquals(19387, node.at("/sentences").asInt());
        assertEquals(514, node.at("/paragraphs").asInt());
        assertEquals(StatusCodes.DEPRECATED, node.at("/warnings/0/0").asInt());
        assertEquals(node.at("/warnings/0/1").asText(), "Parameter corpusQuery is deprecated in favor of cq.");
    }
}
