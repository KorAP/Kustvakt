package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class OAuth2AccessTokenTest extends SpringJerseyTest {

    // test access token for username: dory
    // see:
    // full/src/main/resources/db/insert/V3.5__insert_oauth2_clients.sql
    private static String testAccessToken = "249c64a77f40e2b5504982cc5521b596";

    @Test
    public void testListVC () throws KustvaktException {
        ClientResponse response = resource().path("vc").path("list")
                .header(Attributes.AUTHORIZATION, "Bearer " + testAccessToken)
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(4, node.size());
    }

    @Test
    public void testSearchWithOAuth2Token ()
            throws KustvaktException, IOException {
        ClientResponse response = resource().path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION, "Bearer " + testAccessToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(25, node.at("/matches").size());
    }

    @Test
    public void testSearchWithUnknownToken ()
            throws KustvaktException, IOException {
        ClientResponse response = resource().path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        "Bearer ljsa8tKNRSczJhk20öhq92zG8z350")
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is not found",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testSearchWithExpiredToken ()
            throws KustvaktException, IOException {
        ClientResponse response = resource().path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        "Bearer fia0123ikBWn931470H8s5gRqx7Moc4p")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);

        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.EXPIRED, node.at("/errors/0/0").asInt());
        assertEquals("Access token is expired",
                node.at("/errors/0/1").asText());
    }
}
