package de.ids_mannheim.korap.web.controller.vc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class VirtualCorpusDeprecationTest extends SpringJerseyTest{

	private void testDeprecation (Response response) throws KustvaktException {
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DEPRECATED,
                node.at("/errors/0/0").asInt());
	}
	
	@Test
	public void testV1_0 () throws KustvaktException {
        // list user or system vc
		Response response = target().path(API_VERSION_V1_0).path("vc")
                .path("~dory").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .get();
		testDeprecation(response);
	
		// delete access by id
		response = target().path(API_VERSION_V1_0).path("vc").path("access")
                .path("1").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .delete();
		testDeprecation(response);
	}
	
	@Test
	public void testCurrentVersion () throws KustvaktException {
		 // list user or system vc
		Response response = target().path(API_VERSION).path("vc")
                .path("~dory").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	
		// delete access by id
		response = target().path(API_VERSION).path("vc").path("access")
                .path("1").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .delete();
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}
}
