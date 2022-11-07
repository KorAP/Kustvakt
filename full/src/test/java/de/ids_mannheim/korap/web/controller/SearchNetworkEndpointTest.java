package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class SearchNetworkEndpointTest extends SpringJerseyTest {

    @Autowired
    private KustvaktConfiguration config;

    private ClientAndServer mockServer;
    private MockServerClient mockClient;

    private int port = 6081;
    private String searchResult;
    private String endpointURL = "http://localhost:"+port+"/searchEndpoint";

    public SearchNetworkEndpointTest () throws IOException {
        searchResult = IOUtils.toString(
                ClassLoader.getSystemResourceAsStream(
                        "network-output/search-result.jsonld"),
                StandardCharsets.UTF_8);
    }


    @Before
    public void startMockServer () {
        mockServer = startClientAndServer(port);
        mockClient = new MockServerClient("localhost", mockServer.getPort());
    }


    @After
    public void stopMockServer () {
        mockServer.stop();
    }


    @Test
    public void testSearchNetwork ()
            throws IOException, KustvaktException, URISyntaxException {
        config.setNetworkEndpointURL(endpointURL);
        mockClient.reset()
                .when(request().withMethod("POST").withPath("/searchEndpoint")
                        .withHeaders(
                                new Header("Content-Type",
                                        "application/json; charset=utf-8"),
                                new Header("Accept", "application/json")))
                .respond(response()
                        .withHeader(new Header("Content-Type",
                                "application/json; charset=utf-8"))
                        .withBody(searchResult).withStatusCode(200));

        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("engine", "network")
                .request()
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(2, node.at("/matches").size());
    }


    @Test
    public void testSearchWithUnknownURL ()
            throws IOException, KustvaktException {
        config.setNetworkEndpointURL("http://localhost:1040/search");
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("engine", "network")
                .request()
                .get(ClientResponse.class);
        
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.SEARCH_NETWORK_ENDPOINT_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
    }
    
    @Test
    public void testSearchWithUnknownHost () throws KustvaktException {
        config.setNetworkEndpointURL("http://search.com");
        
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("engine", "network")
                .request()
                .get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.SEARCH_NETWORK_ENDPOINT_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
    }
}
