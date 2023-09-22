package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Search Network Endpoint Test")
class SearchNetworkEndpointTest extends SpringJerseyTest {

    @Autowired
    private KustvaktConfiguration config;

    private ClientAndServer mockServer;

    private MockServerClient mockClient;

    private int port = 6081;

    private String searchResult;

    private String endpointURL = "http://localhost:" + port + "/searchEndpoint";

    public SearchNetworkEndpointTest() throws IOException {
        searchResult = IOUtils.toString(ClassLoader.getSystemResourceAsStream("network-output/search-result.jsonld"), StandardCharsets.UTF_8);
    }

    @BeforeEach
    void startMockServer() {
        mockServer = startClientAndServer(port);
        mockClient = new MockServerClient("localhost", mockServer.getPort());
    }

    @AfterEach
    void stopMockServer() {
        mockServer.stop();
    }

    @Test
    @DisplayName("Test Search Network")
    void testSearchNetwork() throws IOException, KustvaktException, URISyntaxException {
        config.setNetworkEndpointURL(endpointURL);
        mockClient.reset().when(request().withMethod("POST").withPath("/searchEndpoint").withHeaders(new Header("Content-Type", "application/json; charset=utf-8"), new Header("Accept", "application/json"))).respond(response().withHeader(new Header("Content-Type", "application/json; charset=utf-8")).withBody(searchResult).withStatusCode(200));
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("engine", "network").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/matches").size());
    }

    @Test
    @DisplayName("Test Search With Unknown URL")
    void testSearchWithUnknownURL() throws IOException, KustvaktException {
        config.setNetworkEndpointURL("http://localhost:1040/search");
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("engine", "network").request().get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.SEARCH_NETWORK_ENDPOINT_FAILED, node.at("/errors/0/0").asInt());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Test Search With Unknown Host")
    void testSearchWithUnknownHost() throws KustvaktException {
        config.setNetworkEndpointURL("http://search.com");
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("engine", "network").request().get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.SEARCH_NETWORK_ENDPOINT_FAILED, node.at("/errors/0/0").asInt());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
}
