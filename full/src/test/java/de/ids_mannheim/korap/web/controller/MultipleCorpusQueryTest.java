package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class MultipleCorpusQueryTest extends SpringJerseyTest {

    @Test
    public void testSearchGet () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "das").queryParam("ql", "poliqarp")
                .queryParam("cq", "pubPlace=München")
                .queryParam("cq", "textSigle=\"GOE/AGA/01784\"")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        node = node.at("/collection/operands/1");
        assertEquals("koral:docGroup", node.at("/@type").asText());
        assertEquals("operation:and", node.at("/operation").asText());
        assertEquals(2, node.at("/operands").size());
        assertEquals("koral:doc", node.at("/operands/0/@type").asText());
        assertEquals("match:eq", node.at("/operands/0/match").asText());
        assertEquals("pubPlace", node.at("/operands/0/key").asText());
        assertEquals("München", node.at("/operands/0/value").asText());
        assertEquals("textSigle", node.at("/operands/1/key").asText());
        assertEquals("GOE/AGA/01784", node.at("/operands/1/value").asText());
    }

    @Test
    public void testStatisticsWithMultipleCq ()
            throws ProcessingException,
            KustvaktException {
        Response response = target().path(API_VERSION)
                .path("statistics").queryParam("cq", "textType=Abhandlung")
                .queryParam("cq", "corpusSigle=GOE")
                .request()
                .method("GET");
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/documents").asInt());
        assertEquals(138180, node.at("/tokens").asInt());
        assertEquals(5687, node.at("/sentences").asInt());
        assertEquals(258, node.at("/paragraphs").asInt());

        assertTrue(node.at("/warnings").isMissingNode());
    }

    @Test
    public void testStatisticsWithMultipleCorpusQuery ()
            throws ProcessingException,
            KustvaktException {
        Response response =
                target().path(API_VERSION).path("statistics")
                        .queryParam("corpusQuery", "textType=Autobiographie")
                        .queryParam("corpusQuery", "corpusSigle=GOE")
                        .request()
                        .method("GET");
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(9, node.at("/documents").asInt());
        assertEquals(527662, node.at("/tokens").asInt());
        assertEquals(19387, node.at("/sentences").asInt());
        assertEquals(514, node.at("/paragraphs").asInt());

        assertEquals(StatusCodes.DEPRECATED_PARAMETER,
                node.at("/warnings/0/0").asInt());
        assertEquals("Parameter corpusQuery is deprecated in favor of cq.",
                node.at("/warnings/0/1").asText());
    }
}
