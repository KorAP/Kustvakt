package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class MatchInfoControllerTest extends SpringJerseyTest {

    @Test
    public void testGetMatchInfoPublicCorpus () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("GOE").path("AGA").path("01784").path("p36-100")
                .queryParam("foundry", "*").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals(node.at("/textSigle").asText(), "GOE/AGA/01784");
        assertEquals(node.at("/title").asText(), "Belagerung von Mainz");
        assertEquals(node.at("/author").asText(),
                "Goethe, Johann Wolfgang von");
        assertTrue(node.at("/snippet").asText()
                .startsWith("<span class=\"context-left\"></span>"
                        + "<span class=\"match\">"));
    }

    @Test
    public void testGetMatchInfoNotAllowed () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("GOE").path("AGI").path("04846").path("p36875-36876")
                .queryParam("foundry", "*").request().get();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Retrieving resource with ID "
                        + "match-GOE/AGI/04846-p36875-36876 is not allowed.",
                node.at("/errors/0/1").asText());
        assertTrue(node.at("/snippet").isMissingNode());
    }
    
    @Test
    public void testUsingDeprecatedMatchInfoService () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("GOE").path("AGA").path("01784").path("p36-100")
                .path("matchInfo")
                .queryParam("foundry", "*").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DEPRECATED,
                node.at("/warnings/0/0").asInt());
        assertEquals("This service is deprecated. Please use the following "
                + "service URL instead: {version}/corpus/{corpusId}/{docId}/"
                + "{textId}/{matchId}",
                node.at("/warnings/0/1").asText());
    }

    @Test
    public void testGetMatchInfoWithAuthentication () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("GOE").path("AGI").path("04846").path("p36875-36876")
                .queryParam("foundry", "*").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "172.27.0.32").get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals(node.at("/textSigle").asText(), "GOE/AGI/04846");
        assertEquals(node.at("/title").asText(),
                "Zweiter römischer Aufenthalt");
        assertEquals(node.at("/subTitle").asText(),
                "vom Juni 1787 bis April 1788");
        assertEquals(node.at("/author").asText(),
                "Goethe, Johann Wolfgang von");
        assertTrue(node.at("/snippet").asText()
                .startsWith("<span class=\"context-left\"></span>"
                        + "<span class=\"match\">"));
        assertEquals(node.at("/availability").asText(), "QAO-NC-LOC:ids");
    }

    @Test
    public void testAvailabilityAll () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("GOE").path("AGD").path("00000").path("p75-76").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "10.27.0.32").get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testAvailabilityAllUnauthorized () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("GOE").path("AGD").path("00000").path("p75-76").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "170.27.0.32").get();
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Retrieving resource with ID "
                        + "match-GOE/AGD/00000-p75-76 is not allowed.",
                node.at("/errors/0/1").asText());
    }
}
