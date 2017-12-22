package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.jetty.http.HttpHeaders;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.util.MultivaluedMapImpl;

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
        ClientResponse response = resource().path("group").path("list")
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
        ClientResponse response = resource().path("group").path("list")
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

    // marlin does not have any group
    @Test
    public void testRetrieveMarlinGroups () throws KustvaktException {
        ClientResponse response = resource().path("group").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("marlin",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(0, node.size());
    }

    
    @Test
    public void testRetrieveUserGroupUnauthorized () throws KustvaktException {
        ClientResponse response = resource().path("group").path("list")
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

    // marlin has GroupMemberStatus.PENDING in dory group
    @Test
    public void testSubscribeMarlinToDoryGroup () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", "1");

        ClientResponse response = resource().path("group").path("subscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("marlin",
                                "pass"))
                .entity(form).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        
        // retrieve marlin group
        response = resource().path("group").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("marlin",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        entity = response.getEntity(String.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
        assertEquals(1, node.at("/0/id").asInt());
        assertEquals("dory group", node.at("/0/name").asText());
        assertEquals("dory", node.at("/0/owner").asText());
        // group members are not allowed to see other members
        assertEquals(0, node.at("/0/members").size());
    }
    
    // pearl has GroupMemberStatus.DELETED in dory group
    @Test
    public void testSubscribePearlToDoryGroup () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", "1");

        ClientResponse response = resource().path("group").path("subscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("pearl",
                                "pass"))
                .entity(form).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.NOTHING_CHANGED,
                node.at("/errors/0/0").asInt());
        assertEquals("Username pearl had been deleted in group 1",
                node.at("/errors/0/1").asText());
    }
    
    @Test
    public void testSubscribeMissingGroupId () throws KustvaktException {
        ClientResponse response = resource().path("group").path("subscribe")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, handler
                        .createBasicAuthorizationHeaderValue("bruce", "pass"))
                .post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals("groupId", node.at("/errors/0/1").asText());
        assertEquals("0", node.at("/errors/0/2").asText());
    }

    @Test
    public void testSubscribeNonExistentMember () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", "1");

        ClientResponse response = resource().path("group").path("subscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("bruce",
                                "pass"))
                .entity(form).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
//        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.NO_RESULT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Username bruce is not found in group 1",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testSubscribeToNonExistentGroup () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("groupId", "100");

        ClientResponse response = resource().path("group").path("subscribe")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("pearl",
                                "pass"))
                .entity(form).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
//        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.NO_RESULT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Username pearl is not found in group 100",
                node.at("/errors/0/1").asText());
    }

}
