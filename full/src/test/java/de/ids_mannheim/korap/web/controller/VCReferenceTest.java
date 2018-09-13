package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VCReferenceTest extends SpringJerseyTest {

    // auto caching is disabled in krill
    @Ignore
    @Test
    public void testVCRef ()
            throws KustvaktException, IOException, QueryException {
        testSearchWithoutVCRefOr();
        testSearchWithoutVCRefAnd();

        // auto caching
        testStatisticsWithVCReference();
        testSearchWithVCRefNotEqual();
        // retrieve from cache
        testSearchWithVCRefEqual();
        testSearchWithVCRefNotEqual();
    }

    private void testSearchWithoutVCRefOr () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq",
                        "textSigle=\"GOE/AGF/00000\" | textSigle=\"GOE/AGA/01784\"")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
    }

    private void testSearchWithoutVCRefAnd () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq",
                        "textSigle!=\"GOE/AGI/04846\" & textSigle!=\"GOE/AGA/01784\"")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
    }

    public void testSearchWithVCRefEqual () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo named-vc1")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);

    }

    public void testSearchWithVCRefNotEqual () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo named-vc2")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
    }

    public void testStatisticsWithVCReference () throws KustvaktException {
        String corpusQuery = "availability = /CC-BY.*/ & referTo named-vc1";
        ClientResponse response = resource().path(API_VERSION)
                .path("statistics").queryParam("corpusQuery", corpusQuery)
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(2, node.at("/documents").asInt());
    }
}
