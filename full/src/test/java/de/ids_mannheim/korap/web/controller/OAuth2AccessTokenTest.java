package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class OAuth2AccessTokenTest extends SpringJerseyTest {


    private String requestToken () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("client_id", "fCBbQkAyYzI4NzUxMg");
        form.add("client_secret", "secret");
        form.add("username", "dory");
        form.add("password", "password");

        ClientResponse response = resource().path("oauth2").path("token")
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        return node.at("/access_token").asText();
    }

    @Test
    public void testListVC () throws KustvaktException {
        ClientResponse response = resource().path("vc").path("list")
                .header(Attributes.AUTHORIZATION, "Bearer " + requestToken())
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
                .header(Attributes.AUTHORIZATION, "Bearer " + requestToken())
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
}
