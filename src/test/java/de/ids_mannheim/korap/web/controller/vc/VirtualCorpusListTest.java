package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import de.ids_mannheim.korap.util.QueryException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

public class VirtualCorpusListTest extends VirtualCorpusTestBase {

    @Test
    public void testListVCNemo ()
            throws ProcessingException, KustvaktException {
    	createNemoVC();
        JsonNode node = testListOwnerVC("nemo");
        assertEquals(1, node.size());
        node = listSystemVC("nemo");
        assertEquals(1, node.size());
        node = listVC("nemo");
        assertEquals(2, node.size());
        deleteVC("nemo-vc", "nemo", "nemo");
    }

    @Test
    public void testListVCPearl ()
            throws ProcessingException, KustvaktException, IOException, QueryException {
        JsonNode node = testListOwnerVC("pearl");
        assertEquals(0, node.size());
        node = listVC("pearl");
        assertEquals(1, node.size());
        node = node.get(0);
		assertEquals("system-vc", node.at("/name").asText());
		assertEquals("system", node.at("/type").asText());
		assertEquals(CorpusAccess.ALL.name(),
				node.at("/requiredAccess").asText());
		assertEquals("system", node.at("/createdBy").asText());
        assertEquals(11,node.at("/numberOfDoc").asInt());
        assertEquals(772,node.at("/numberOfParagraphs").asInt());
        assertEquals(25074,node.at("/numberOfSentences").asInt());
        assertEquals(665842,node.at("/numberOfTokens").asInt());
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
    	createDoryVC();
    	createDoryGroupVC();
    	
        JsonNode node = testListOwnerVC("dory");
        assertEquals(2, node.size());
        node = listVC("dory");
        assertEquals(3, node.size());
        
        deleteVC("dory-vc", "dory", "dory");
        deleteVC("group-vc", "dory", "dory");
    }

    @Test
    public void testListAvailableVCGuest ()
            throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").request()
                .get();
        testResponseUnauthorized(response, "guest");
    }
}
