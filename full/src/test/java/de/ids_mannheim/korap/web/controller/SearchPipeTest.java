package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class SearchPipeTest extends SpringJerseyTest {

    private ClientAndServer mockServer;
    private MockServerClient mockClient;

    private String pipeJson, pipeWithParamJson;
    private String glemmUri = "http://localhost:1080/glemm";

    public SearchPipeTest () throws URISyntaxException, IOException {
        pipeJson = IOUtils.toString(
                ClassLoader.getSystemResourceAsStream(
                        "pipe-output/test-pipes.jsonld"),
                StandardCharsets.UTF_8);

        pipeWithParamJson = IOUtils.toString(
                ClassLoader.getSystemResourceAsStream(
                        "pipe-output/with-param.jsonld"),
                StandardCharsets.UTF_8);
    }

    @Before
    public void startMockServer () {
        mockServer = startClientAndServer(1080);
        mockClient = new MockServerClient("localhost", mockServer.getPort());
    }

    @After
    public void stopMockServer () {
        mockServer.stop();
    }

    @Test
    public void testMockServer () throws IOException {
        mockClient.reset()
                .when(request().withMethod("POST").withPath("/test")
                        .withHeader(new Header("Content-Type",
                                "application/json; charset=utf-8")))
                .respond(response()
                        .withHeader(new Header("Content-Type",
                                "application/json; charset=utf-8"))
                        .withBody("{test}").withStatusCode(200));

        URL url = new URL("http://localhost:1080/test");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        String json = "{\"name\" : \"dory\"}";
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = json.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        assertEquals(200, connection.getResponseCode());

        BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "utf-8"));
        assertEquals("{test}", br.readLine());

    }

    @Test
    public void testSearchWithPipes ()
            throws IOException, KustvaktException, URISyntaxException {
        mockClient.reset()
                .when(request().withMethod("POST").withPath("/glemm")
                        .withHeaders(
                                new Header("Content-Type",
                                        "application/json; charset=utf-8"),
                                new Header("Accept", "application/json")))
                .respond(response()
                        .withHeader(new Header("Content-Type",
                                "application/json; charset=utf-8"))
                        .withBody(pipeJson).withStatusCode(200));

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
    public void testSearchWithUrlEncodedPipes ()
            throws IOException, KustvaktException {

        mockClient.reset()
                .when(request().withMethod("POST").withPath("/glemm")
                        .withHeaders(
                                new Header("Content-Type",
                                        "application/json; charset=utf-8"),
                                new Header("Accept", "application/json")))
                .respond(response()
                        .withHeader(new Header("Content-Type",
                                "application/json; charset=utf-8"))
                        .withBody(pipeJson).withStatusCode(200));

        glemmUri = URLEncoder.encode(glemmUri, "utf-8");

        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", glemmUri).get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/query/wrap/key").size());
    }

    @Test
    public void testSearchWithMultiplePipes () throws KustvaktException {

        mockClient.reset()
                .when(request().withMethod("POST").withPath("/glemm")
                        .withQueryStringParameter("param").withHeaders(
                                new Header("Content-Type",
                                        "application/json; charset=utf-8"),
                                new Header("Accept", "application/json")))
                .respond(response()
                        .withHeader(new Header("Content-Type",
                                "application/json; charset=utf-8"))
                        .withBody(pipeWithParamJson).withStatusCode(200));

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
        assertEquals("404 Not Found", node.at("/warnings/0/3").asText());
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
        assertEquals("glemm", node.at("/warnings/0/3").asText());
    }

    @Test
    public void testSearchUnsupportedMediaType () throws KustvaktException {
        mockClient.reset()
                .when(request().withMethod("POST").withPath("/non-json-pipe"))
                .respond(response().withStatusCode(415));

        String pipeUri = "http://localhost:1080/non-json-pipe";

        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", pipeUri).get(ClientResponse.class);

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
        assertEquals("glemm", node.at("/warnings/1/3").asText());

    }

    @Test
    public void testSearchWithInvalidJsonResponse () throws KustvaktException {
        mockClient.reset()
                .when(request().withMethod("POST").withPath("/invalid-response")
                        .withHeaders(
                                new Header("Content-Type",
                                        "application/json; charset=utf-8"),
                                new Header("Accept", "application/json")))
                .respond(response().withBody("{blah:}").withStatusCode(200)
                        .withHeaders(new Header("Content-Type",
                                "application/json; charset=utf-8")));

        String pipeUri = "http://localhost:1080/invalid-response";
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", pipeUri).get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void testSearchWithPlainTextResponse () throws KustvaktException {
        mockClient.reset()
                .when(request().withMethod("POST").withPath("/plain-text")
                        .withHeaders(
                                new Header("Content-Type",
                                        "application/json; charset=utf-8"),
                                new Header("Accept", "application/json")))
                .respond(response().withBody("blah").withStatusCode(200));

        String pipeUri = "http://localhost:1080/plain-text";
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", pipeUri).get(ClientResponse.class);

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

        mockClient.reset()
                .when(request().withMethod("POST").withPath("/glemm")
                        .withHeaders(
                                new Header("Content-Type",
                                        "application/json; charset=utf-8"),
                                new Header("Accept", "application/json")))
                .respond(response()
                        .withHeader(new Header("Content-Type",
                                "application/json; charset=utf-8"))
                        .withBody(pipeJson).withStatusCode(200));

        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", "http://unknown" + "," + glemmUri)
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/query/wrap/key").size());
        assertTrue(node.at("/warnings").isMissingNode());

        response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", glemmUri + ",http://unknown")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        entity = response.getEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/0/0").asInt());
    }
}
