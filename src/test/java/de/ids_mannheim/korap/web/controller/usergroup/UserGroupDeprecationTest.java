package de.ids_mannheim.korap.web.controller.usergroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class UserGroupDeprecationTest extends SpringJerseyTest{

	private String groupName= "GroupV1_0";
	
	private void testDeprecation (Response response) throws KustvaktException {
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DEPRECATED,
                node.at("/errors/0/0").asInt());
	}
	
	@Test
	public void testV1_0 () throws KustvaktException {
		Form form = new Form();
        form.param("members", "marlin");
        
        // invite member
		Response response = target().path(API_VERSION_V1_0).path("group")
                .path("@" + groupName+"/invite").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(form));
		testDeprecation(response);
	
		// add member role
		response = target().path(API_VERSION_V1_0).path("group")
                .path("@" + groupName+"/role/add").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(form));
		testDeprecation(response);
		
		// subscribe
		response = target().path(API_VERSION_V1_0).path("group")
                .path("@" + groupName+"/subscribe").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(form));
		testDeprecation(response);

		// unsusbcribe
		response = target().path(API_VERSION_V1_0).path("group")
                .path("@" + groupName+"/unsubscribe").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .delete();
		testDeprecation(response);
	}
	
	@Test
	public void testCurrentVersion () throws KustvaktException {
		Form form = new Form();
        form.param("members", "marlin");
        
        // invite member
		Response response = target().path(API_VERSION).path("group")
                .path("@" + groupName+"/invite").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(form));
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	
		// add member role
		response = target().path(API_VERSION).path("group")
                .path("@" + groupName+"/role/add").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(form));
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
		
		// subscribe
		response = target().path(API_VERSION).path("group")
                .path("@" + groupName+"/subscribe").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .post(Entity.form(form));
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());

		// unsusbcribe
		response = target().path(API_VERSION).path("group")
                .path("@" + groupName+"/unsubscribe").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("dory", "pass"))
                .delete();
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}
}
