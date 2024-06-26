package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

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
                .queryParam("cq", "textSigle=\"GOE/AGA/01784\"").request()
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        node = node.at("/collection/operands/1");
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
    public void testStatisticsWithMultipleCq ()
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics")
                .queryParam("cq", "textType=Abhandlung")
                .queryParam("cq", "corpusSigle=GOE").request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/documents").asInt());
        assertEquals(138180, node.at("/tokens").asInt());
        assertEquals(5687, node.at("/sentences").asInt());
        assertEquals(258, node.at("/paragraphs").asInt());
        assertTrue(node.at("/warnings").isMissingNode());
    }
}
