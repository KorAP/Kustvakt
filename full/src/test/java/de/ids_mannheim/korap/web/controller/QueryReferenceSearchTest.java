package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class QueryReferenceSearchTest {

    /*@Test
    public void testSearchWithVCRefEqual () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"dory/dory-q\"")
                .get();

        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);
    }
*/    
}
