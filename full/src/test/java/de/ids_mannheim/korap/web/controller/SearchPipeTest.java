package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URLEncoder;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class SearchPipeTest extends SpringJerseyTest {

    @Test
    public void testSearchWithPipes () throws IOException, KustvaktException {
        String glemmUri =
                resource().getURI().toString() + API_VERSION + "/test/glemm";
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", glemmUri).get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/query/wrap/key").size());

        assertEquals(1, node.at("/collection/rewrites").size());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
        assertEquals("availability(FREE)",
                node.at("/collection/rewrites/0/scope").asText());

        node = node.at("/query/wrap/rewrites");
        assertEquals(2, node.size());
        assertEquals("Glemm", node.at("/0/src").asText());
        assertEquals("operation:override", node.at("/0/operation").asText());
        assertEquals("key", node.at("/0/scope").asText());

        assertEquals("Kustvakt", node.at("/1/src").asText());
        assertEquals("operation:injection", node.at("/1/operation").asText());
        assertEquals("foundry", node.at("/1/scope").asText());
    }
    
    @Test
    public void testSearchWithUrlEncodedPipes () throws IOException, KustvaktException {
        String glemmUri =
                resource().getURI().toString() + API_VERSION + "/test/glemm";
        glemmUri = URLEncoder.encode(glemmUri,"utf-8");
        
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", glemmUri).get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/query/wrap/key").size());
    }

    @Test
    public void testSearchWithMultiplePipes () throws KustvaktException {
        String glemmUri =
                resource().getURI().toString() + API_VERSION + "/test/glemm";
        String glemmUri2 = glemmUri + "?param=blah";
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", glemmUri + "," + glemmUri2)
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(3, node.at("/query/wrap/key").size());
    }

    @Test
    public void testSearchWithUnknownURL ()
            throws IOException, KustvaktException {
        String url =
                resource().getURI().toString() + API_VERSION + "/test/tralala";
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", url).get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/0/0").asInt());
    }

    @Test
    public void testSearchWithUnknownHost () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", "http://glemm").get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/0/0").asInt());
    }

    @Test
    public void testSearchWithUrlEncodedPipe () throws KustvaktException {
        String pipe = resource().getURI().toString() + API_VERSION
                + "/test/urlencoded-pipe";
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", pipe).get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/0/0").asInt());
        assertEquals("415 Unsupported Media Type",
                node.at("/warnings/0/3").asText());
    }

    @Test
    public void testSearchWithMultiplePipeWarnings () throws KustvaktException {
        String url =
                resource().getURI().toString() + API_VERSION + "/test/tralala";
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", url + "," + "http://glemm")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(2, node.at("/warnings").size());
        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/0/0").asInt());
        assertEquals(url, node.at("/warnings/0/2").asText());
        assertEquals("404 Not Found", node.at("/warnings/0/3").asText());

        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/1/0").asInt());
        assertEquals("http://glemm", node.at("/warnings/1/2").asText());
        assertEquals("java.net.UnknownHostException: glemm",
                node.at("/warnings/1/3").asText());

    }

    @Test
    public void testSearchWithInvalidJsonResponse () throws KustvaktException {
        String pipe = resource().getURI().toString() + API_VERSION
                + "/test/invalid-json-pipe";
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", pipe).get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testSearchWithPlainTextResponse () throws KustvaktException {
        String pipe = resource().getURI().toString() + API_VERSION
                + "/test/plain-response-pipe";
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", pipe).get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testSearchWithMultipleAndUnknownPipes ()
            throws KustvaktException {
        String glemmUri =
                resource().getURI().toString() + API_VERSION + "/test/glemm";
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", "http://unknown" + "," + glemmUri)
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/0/0").asInt());
        assertEquals(2, node.at("/query/wrap/key").size());
    }
}
