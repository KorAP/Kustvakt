package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ContainerRequest;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.AuthenticationScheme;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.service.VirtualCorpusServiceTest;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VirtualCorpusControllerTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;

    private void checkWWWAuthenticateHeader (ClientResponse response) {
        Set<Entry<String, List<String>>> headers =
                response.getHeaders().entrySet();

        for (Entry<String, List<String>> header : headers) {
            if (header.getKey().equals(ContainerRequest.WWW_AUTHENTICATE)) {
                assertEquals("Api realm=\"Kustvakt\"",
                        header.getValue().get(0));
                assertEquals("Session realm=\"Kustvakt\"",
                        header.getValue().get(1));
                assertEquals("Bearer realm=\"Kustvakt\"",
                        header.getValue().get(2));
                assertEquals("Basic realm=\"Kustvakt\"",
                        header.getValue().get(3));
            }
        }
    }

    private JsonNode testSearchVC (String username, String vcId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response =
                resource().path("vc").path("search").path(vcId)
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        username, "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        return JsonUtils.readTree(entity);
    }

    private JsonNode testListVC (String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path("vc").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity(String.class);
        //                System.out.println(entity);
        return JsonUtils.readTree(entity);
    }


    private JsonNode testListOwnerVC (String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response =
                resource().path("vc").path("list").path("user")
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        username, "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                        .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        return JsonUtils.readTree(entity);
    }

    private void testDeleteVC (String vcId, String username)
            throws KustvaktException {
        ClientResponse response =
                resource().path("vc").path("delete").path(vcId)
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        username, "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                        .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private JsonNode testlistAccessByVC (String username, String vcId)
            throws KustvaktException {
        ClientResponse response = resource().path("vc").path("access")
                .path("list").queryParam("vcId", vcId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //                System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        return node;
    }

    @Test
    public void testSearchSystemVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        JsonNode node = testSearchVC("VirtualCorpusControllerTest", "3");
        assertEquals("system VC", node.at("/name").asText());
        assertEquals(VirtualCorpusType.SYSTEM.displayName(),
                node.at("/type").asText());
    }

    @Test
    public void testSearchOwnerPrivateVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        JsonNode node = testSearchVC("dory", "1");
        assertEquals("dory VC", node.at("/name").asText());
        assertEquals(VirtualCorpusType.PRIVATE.displayName(),
                node.at("/type").asText());
    }

    @Test
    public void testSearchPrivateVCUnauthorized ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path("vc").path("search").path("1")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Unauthorized operation for user: VirtualCorpusControllerTest",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testSearchProjectVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        JsonNode node = testSearchVC("nemo", "2");
        assertEquals("group VC", node.at("/name").asText());
        assertEquals(VirtualCorpusType.PROJECT.displayName(),
                node.at("/type").asText());
    }

    @Test
    public void testSearchProjectVCNonActiveMember ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        ClientResponse response = resource().path("vc").path("search").path("2")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("marlin",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: marlin",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testSearchPublishedVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        JsonNode node = testSearchVC("gill", "4");
        assertEquals("published VC", node.at("/name").asText());
        assertEquals(VirtualCorpusType.PUBLISHED.displayName(),
                node.at("/type").asText());

        // EM: need admin to check if VirtualCorpusControllerTest is added to the hidden group
    }

    @Test
    public void testListNemoVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode node = testListVC("nemo");
        assertEquals(3, node.size());

    }

    @Test
    public void testListPearlVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode node = testListVC("pearl");
        assertEquals(2, node.size());

    }

    @Test
    public void testListDoryVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        JsonNode node = testListVC("dory");
        assertEquals(4, node.size());

    }

    @Test
    public void testListVCByOtherUser () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("vc").path("list")
                .queryParam("createdBy", "dory")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, handler
                        .createBasicAuthorizationHeaderValue("pearl", "pass"))
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: pearl",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testListVCByGuest () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("vc").path("list")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: guest",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreatePrivateVC () throws KustvaktException {
        String json = "{\"name\": \"new vc\",\"type\": \"PRIVATE\","
                + "\"corpusQuery\": \"corpusSigle=GOE\"}";

        ClientResponse response = resource().path("vc").path("create")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // list user VC
        JsonNode node = testListVC("VirtualCorpusControllerTest");
        assertEquals(2, node.size());
        assertEquals("new vc", node.get(1).get("name").asText());

        String vcId = null;
        vcId = node.get(1).get("id").asText();

        // delete new VC
        testDeleteVC(vcId, "VirtualCorpusControllerTest");

        // list VC
        node = testListVC("VirtualCorpusControllerTest");
        assertEquals(1, node.size());
    }

    @Test
    public void testCreatePublishVC () throws KustvaktException {
        String json = "{\"name\": \"new published vc\",\"type\": \"PUBLISHED\""
                + ",\"corpusQuery\": \"corpusSigle=GOE\"}";
        ClientResponse response = resource().path("vc").path("create")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // test list owner vc
        JsonNode node = testListOwnerVC("VirtualCorpusControllerTest");
        assertEquals(1, node.size());
        assertEquals("new published vc", node.get(0).get("name").asText());

        String vcId = node.get(0).get("id").asText();

        // EM: check hidden access
        node = testlistAccessByVC("admin", vcId).get(0);
        assertEquals("system", node.at("/createdBy").asText());
        assertEquals(vcId, node.at("/vcId").asText());
        assertEquals("auto-hidden-group", node.at("/userGroupName").asText());
        assertEquals("new published vc", node.at("/vcName").asText());

        String groupId = node.at("/userGroupId").asText();

        // EM: check if hidden group has been created
        node = testCheckHiddenGroup(groupId);
        assertEquals("HIDDEN", node.at("/status").asText());

        //EM: delete vc
        testDeleteVC(vcId, "VirtualCorpusControllerTest");

        //EM: check if the hidden groups are deleted as well
        node = testCheckHiddenGroup(groupId);
        assertEquals(StatusCodes.GROUP_NOT_FOUND,
                node.at("/errors/0/0").asInt());
        assertEquals("Group with id 5 is not found",
                node.at("/errors/0/1").asText());
    }

    private JsonNode testCheckHiddenGroup (String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path("group").path(groupId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("admin",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    @Test
    public void testCreateVCWithExpiredToken ()
            throws IOException, KustvaktException {
        String json = "{\"name\": \"new vc\",\"type\": \"PRIVATE\","
                + "\"corpusQuery\": \"corpusSigle=GOE\"}";

        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("test-user.token");

        String authToken;
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(is));) {
            authToken = reader.readLine();
        }

        ClientResponse response = resource().path("vc").path("create")
                .header(Attributes.AUTHORIZATION,
                        AuthenticationScheme.API.displayName() + " "
                                + authToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);
        String entity = response.getEntity(String.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.EXPIRED, node.at("/errors/0/0").asInt());
        assertEquals("Authentication token is expired",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateSystemVC () throws KustvaktException {
        String json = "{\"name\": \"new vc\",\"type\": \"SYSTEM\","
                + "\"corpusQuery\": \"creationDate since 1820\"}";

        ClientResponse response = resource().path("vc").path("create")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Unauthorized operation for user: VirtualCorpusControllerTest",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateVCUnauthorized () throws KustvaktException {
        String json = "{\"name\": \"new vc\",\"type\": \"PRIVATE\","
                + "\"corpusQuery\": \"creationDate since 1820\"}";

        ClientResponse response = resource().path("vc").path("create")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: guest",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateVCWithoutcorpusQuery () throws KustvaktException {
        String json = "{\"name\": \"new vc\",\"type\": \"PRIVATE\"}";

        ClientResponse response = resource().path("vc").path("create")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals("corpusQuery", node.at("/errors/0/1").asText());
        assertEquals("null", node.at("/errors/0/2").asText());
    }

    @Test
    public void testCreateVCWithoutType () throws KustvaktException {
        String json = "{\"name\": \"new vc\",\"corpusQuery\": "
                + "\"creationDate since 1820\"}";

        ClientResponse response = resource().path("vc").path("create")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals("type", node.at("/errors/0/1").asText());
        assertEquals("null", node.at("/errors/0/2").asText());
    }

    @Test
    public void testCreateVCWithWrongType () throws KustvaktException {
        String json = "{\"name\": \"new vc\",\"type\": \"PRIVAT\","
                + "\"corpusQuery\": \"creationDate since 1820\"}";

        ClientResponse response = resource().path("vc").path("create")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertTrue(node.at("/errors/0/1").asText().startsWith(
                "Cannot deserialize value of type `de.ids_mannheim.korap.constant."
                        + "VirtualCorpusType` from String \"PRIVAT\": value not one of "
                        + "declared Enum instance names"));
    }

    @Test
    public void testDeleteVCUnauthorized () throws KustvaktException {
        ClientResponse response = resource().path("vc").path("delete").path("1")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .delete(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Unauthorized operation for user: VirtualCorpusControllerTest",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testEditVC () throws KustvaktException {

        // 1st edit
        String json = "{\"id\": \"1\", \"name\": \"edited vc\"}";

        ClientResponse response = resource().path("vc").path("edit")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check VC
        JsonNode node = testListVC("dory");
        assertEquals("edited vc", node.get(0).get("name").asText());

        // 2nd edit
        json = "{\"id\": \"1\", \"name\": \"dory VC\"}";

        response = resource().path("vc").path("edit")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check VC
        node = testListVC("dory");
        assertEquals("dory VC", node.get(0).get("name").asText());
    }

    @Test
    public void testEditVCNotOwner () throws KustvaktException {
        String json = "{\"id\": \"1\", \"name\": \"edited vc\"}";

        ClientResponse response = resource().path("vc").path("edit")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Unauthorized operation for user: VirtualCorpusControllerTest",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }


    /**
     * @see VirtualCorpusServiceTest
     * @throws KustvaktException
     */
    @Test
    public void testEditPublishVC () throws KustvaktException {

        String vcId = "2";
        String json = "{\"id\": \"" + vcId + "\", \"type\": \"PUBLISHED\"}";

        ClientResponse response = resource().path("vc").path("edit")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check VC
        JsonNode node = testListOwnerVC("dory");
        JsonNode n = node.get(1);
        assertEquals(VirtualCorpusType.PUBLISHED.displayName(),
                n.get("type").asText());

        //check hidden VC access
        node = testlistAccessByVC("admin", vcId);
        assertEquals(2, node.size());

        // edit 2nd
        json = "{\"id\": \"2\", \"type\": \"PROJECT\"}";

        response = resource().path("vc").path("edit")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        node = testListOwnerVC("dory");
        assertEquals(VirtualCorpusType.PROJECT.displayName(),
                node.get(1).get("type").asText());

        //check VC access
        node = testlistAccessByVC("admin", vcId);
        assertEquals(1, node.size());
    }

    @Test
    public void testlistAccessNonVCAAdmin () throws KustvaktException {
        JsonNode node = testlistAccessByVC("nemo", "2");
        assertEquals(0, node.size());
    }

    @Test
    public void testlistAccessMissingId () throws KustvaktException {
        ClientResponse response =
                resource().path("vc").path("access").path("list")
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        "VirtualCorpusControllerTest", "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.MISSING_ARGUMENT,
                node.at("/errors/0/0").asInt());
        assertEquals("vcId", node.at("/errors/0/1").asText());
    }

    @Test
    public void testlistAccessByGroup () throws KustvaktException {
        ClientResponse response = resource().path("vc").path("access")
                .path("list").path("byGroup").queryParam("groupId", "2")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.at("/0/accessId").asInt());
        assertEquals(2, node.at("/0/vcId").asInt());
        assertEquals("group VC", node.at("/0/vcName").asText());
        assertEquals(2, node.at("/0/userGroupId").asInt());
        assertEquals("dory group", node.at("/0/userGroupName").asText());
    }


    @Test
    public void testCreateDeleteAccess () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        String vcId = "5";

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        // marlin vc
        form.add("vcId", vcId);
        // marlin group
        form.add("groupId", "1");

        ClientResponse response;
        // share VC
        response = resource().path("vc").path("access").path("share")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("marlin",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(form)
                .post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // list vc access by marlin
        JsonNode node = testlistAccessByVC("marlin", vcId);
        assertEquals(1, node.size());
        node = node.get(0);
        assertEquals(5, node.at("/vcId").asInt());
        assertEquals("marlin VC", node.at("/vcName").asText());
        assertEquals(1, node.at("/userGroupId").asInt());
        assertEquals("marlin group", node.at("/userGroupName").asText());

        String accessId = node.at("/accessId").asText();

        // delete access
        // unauthorized
        testDeleteAccessUnauthorized(accessId);

        // delete access
        // dory is a vc-admin in marlin group
        testDeleteAccess("dory", accessId);

        // list vc access by dory
        node = testlistAccessByVC("dory", vcId);
        assertEquals(0, node.size());
    }

    @Test
    public void testCreateAccessByVCAButNotVCOwner ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        // marlin vc
        form.add("vcId", "5");
        // marlin group
        form.add("groupId", "1");

        // share VC
        // dory is VCA in marlin group 
        ClientResponse response = resource().path("vc").path("access")
                .path("share").type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(form)
                .post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: dory",
                node.at("/errors/0/1").asText());
    }

    @Test
    public void testCreateAccessByNonVCA () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        // nemo vc
        form.add("vcId", "6");
        // marlin group
        form.add("groupId", "1");

        // share VC
        // nemo is not VCA in marlin group 
        ClientResponse response = resource().path("vc").path("access")
                .path("share").type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("nemo",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(form)
                .post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Unauthorized operation for user: nemo",
                node.at("/errors/0/1").asText());
    }

    private void testDeleteAccess (String username, String accessId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path("vc").path("access")
                .path("delete").path(accessId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testDeleteAccessUnauthorized (String accessId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response = resource().path("vc").path("access")
                .path("delete").path(accessId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(
                "Unauthorized operation for user: VirtualCorpusControllerTest",
                node.at("/errors/0/1").asText());
    }
}
