package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

public class VirtualCorpusListTest extends VirtualCorpusTestBase {

    @Test
    public void testListVCNemo ()
            throws ProcessingException, KustvaktException {
        JsonNode node = testListOwnerVC("nemo");
        assertEquals(1, node.size());
        node = listSystemVC("nemo");
        assertEquals(1, node.size());
        node = listVC("nemo");
        assertEquals(2, node.size());
    }

    @Test
    public void testListVCPearl ()
            throws ProcessingException, KustvaktException {
        JsonNode node = testListOwnerVC("pearl");
        assertEquals(0, node.size());
        node = listVC("pearl");
        assertEquals(1, node.size());
        node = node.get(0);
		assertEquals("system-vc", node.at("/name").asText());
		assertEquals("system", node.at("/type").asText());
		assertEquals("experimental", node.at("/status").asText());
		assertEquals(CorpusAccess.ALL.name(),
				node.at("/requiredAccess").asText());
		assertEquals("system", node.at("/createdBy").asText());
        assertTrue(node.at("/numberOfDoc").isMissingNode());
        assertTrue(node.at("/numberOfParagraphs").isMissingNode());
        assertTrue(node.at("/numberOfSentences").isMissingNode());
        assertTrue(node.at("/numberOfTokens").isMissingNode());
    }

    @Test
    public void testListVCMarlin ()
            throws ProcessingException, KustvaktException {
        JsonNode node = testListOwnerVC("marlin");
        assertEquals(2, node.size());
        node = listVC("marlin");
        assertEquals(3, node.size());
    }

    
    @Test
    public void testListVCDory ()
            throws ProcessingException, KustvaktException {
        JsonNode node = testListOwnerVC("dory");
        assertEquals(2, node.size());
        node = listVC("dory");
        assertEquals(3, node.size());
    }

    @Test
    public void testListAvailableVCGuest ()
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").request()
                .get();
        testResponseUnauthorized(response, "guest");
    }
}
