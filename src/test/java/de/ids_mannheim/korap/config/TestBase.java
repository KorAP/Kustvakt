package de.ids_mannheim.korap.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public abstract class TestBase extends SpringJerseyTest {

	protected Response createUpdateDefaultSettings (String username, Map<String, Object> map)
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("~" + username)
                .path("setting").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .put(Entity.json(map));
        return response;
    }
	
	protected void testDeleteSetting (String username) throws KustvaktException {
        Response response = target().path(API_VERSION).path("~" + username)
                .path("setting").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response = target().path(API_VERSION).path("~" + username)
                .path("setting").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals(username, node.at("/errors/0/2").asText());
    }
}
