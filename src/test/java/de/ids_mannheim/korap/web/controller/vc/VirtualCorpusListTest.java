package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

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
