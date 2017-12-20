package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;

import org.eclipse.jetty.http.HttpHeaders;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

public class UserGroupServiceTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;

    // dory is a group admin in dory group
    @Test
    public void testRetrieveDoryGroups () throws KustvaktException {
        ClientResponse response = resource().path("group").path("user")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
//        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals(1, node.at("/0/id").asInt());
        assertEquals("dory group", node.at("/0/name").asText());
        assertEquals("dory", node.at("/0/owner").asText());
        assertEquals(3, node.at("/0/members").size());
    }
    
    // nemo is a group member in dory group
    @Test
    public void testRetrieveNemoGroups () throws KustvaktException {
        ClientResponse response = resource().path("group").path("user")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("nemo",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
//        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals(1, node.at("/0/id").asInt());
        assertEquals("dory group", node.at("/0/name").asText());
        assertEquals("dory", node.at("/0/owner").asText());
        // group members are not allowed to see other members
        assertEquals(0, node.at("/0/members").size());
    }
    
    @Test
    public void testRetrieveUserGroupUnauthorized () throws KustvaktException {
        ClientResponse response = resource().path("group").path("user")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
//        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Operation is not permitted for user: guest",
                node.at("/errors/0/1").asText());
        
    }
}
