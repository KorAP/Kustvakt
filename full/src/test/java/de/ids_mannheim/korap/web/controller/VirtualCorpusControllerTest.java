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

import org.apache.http.entity.ContentType;
import org.eclipse.jetty.http.HttpHeaders;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.spi.container.ContainerRequest;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.AuthenticationScheme;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
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

    @Test
    public void tesListVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("vc").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        //                System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(3, node.size());
    }

    @Test
    public void testListVCUnauthorized () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("vc").path("list")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Operation is not permitted for user: guest",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateDeleteVC () throws KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVATE\",\"createdBy\": "
                        + "\"VirtualCorpusControllerTest\",\"corpusQuery\": \"corpusSigle=GOE\"}";

        ClientResponse response = resource().path("vc").path("create")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);
        String entity = response.getEntity(String.class);
        //        System.out.println(entity);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // retrieve user VC
        response = resource().path("vc").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .get(ClientResponse.class);
        entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
//        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
//        EM: order may be different
//        assertEquals("new vc", node.get(0).get("name").asText());

        String vcId = null;
        for (int i=0; i<node.size(); i++){
            if (node.get(i).get("name").asText().equals("new vc")){
                vcId = node.get(i).get("id").asText();
            }
        }         

        // delete new VC
        resource().path("vc").path("delete").queryParam("vcId", vcId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .delete(ClientResponse.class);
        //        entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // list VC
        response = resource().path("vc").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .get(ClientResponse.class);
        entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        //        System.out.println(entity);
        node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
    }

    @Test
    public void testCreatePublishVC () throws KustvaktException {
        String json =
                "{\"name\": \"new published vc\",\"type\": \"PUBLISHED\",\"createdBy\": "
                        + "\"VirtualCorpusControllerTest\",\"corpusQuery\": \"corpusSigle=GOE\"}";
        ClientResponse response = resource().path("vc").path("create")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // test list owner vc
        response = resource().path("vc").path("list").path("user")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
        assertEquals("new published vc", node.get(0).get("name").asText());
        //EM: cannot explicitly checked hidden groups here

        String vcId = node.get(0).get("id").asText();

        //EM: delete vc
        resource().path("vc").path("delete").queryParam("vcId", vcId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(
                                "VirtualCorpusControllerTest", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .delete(ClientResponse.class);

        //EM: have to delete the hidden groups as well (admin)
    }

    @Test
    public void testCreateVCWithExpiredToken ()
            throws IOException, KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVATE\",\"createdBy\": "
                        + "\"VirtualCorpusControllerTest\",\"corpusQuery\": \"corpusSigle=GOE\"}";

        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("test-user.token");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String authToken = reader.readLine();

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
    public void testCreateVCUnauthorized () throws KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVATE\",\"createdBy\": "
                        + "\"VirtualCorpusControllerTest\",\"corpusQuery\": \"creationDate since 1820\"}";

        ClientResponse response = resource().path("vc").path("create")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Operation is not permitted for user: guest",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testCreateVCWithoutcorpusQuery () throws KustvaktException {
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVATE\",\"createdBy\": "
                        + "\"VirtualCorpusControllerTest\"}";

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
        String json = "{\"name\": \"new vc\",\"createdBy\": "
                + "\"VirtualCorpusControllerTest\",\"corpusQuery\": \"creationDate since 1820\"}";

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
        String json =
                "{\"name\": \"new vc\",\"type\": \"PRIVAT\",\"createdBy\": "
                        + "\"VirtualCorpusControllerTest\",\"corpusQuery\": \"creationDate since 1820\"}";

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
        ClientResponse response =
                resource().path("vc").path("delete").queryParam("vcId", "1")
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
        assertEquals("Unauthorized operation for user: VirtualCorpusControllerTest",
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
        response = resource().path("vc").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);

        for (int i = 0; i < node.size(); i++) {
            JsonNode n = node.get(i);
            if (n.get("id").asInt() == 1) {
                assertEquals("edited vc", n.get("name").asText());
                break;
            }
        }

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
        response = resource().path("vc").path("list")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        entity = response.getEntity(String.class);
        node = JsonUtils.readTree(entity);

        for (int i = 0; i < node.size(); i++) {
            JsonNode n = node.get(i);
            if (n.get("id").asInt() == 1) {
                assertEquals("dory VC", n.get("name").asText());
                break;
            }
        }
    }

    @Test
    public void testEditPublishVC () throws KustvaktException {

        String json =
                "{\"id\": \"1\", \"name\": \"dory published vc\", \"type\": \"PUBLISHED\"}";

        ClientResponse response = resource().path("vc").path("edit")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // check VC
        response = resource().path("vc").path("list").path("user")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")

                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);

        for (int i = 0; i < node.size(); i++) {
            JsonNode n = node.get(i);
            if (n.get("id").asInt() == 1) {
                assertEquals("dory published vc", n.get("name").asText());
                assertEquals(VirtualCorpusType.PUBLISHED.displayName(),
                        n.get("type").asText());
                break;
            }
        }
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
        assertEquals("Unauthorized operation for user: VirtualCorpusControllerTest",
                node.at("/errors/0/1").asText());

        checkWWWAuthenticateHeader(response);
    }

    @Test
    public void testlistAccessByVC () throws KustvaktException {
        ClientResponse response =
                resource().path("vc").path("access").path("list")
                        .queryParam("vcId", "2")
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        "dory", "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_JSON)
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
    public void testlistAccessNonVCAAdmin () throws KustvaktException {
        ClientResponse response =
                resource().path("vc").path("access").path("list")
                        .queryParam("vcId", "2")
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        "nemo", "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_JSON)
                        .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        assertEquals("[]", entity);       
    }

    @Test
    public void testlistAccessMissingId () throws KustvaktException {
        ClientResponse response =
                resource().path("vc").path("access").path("list")
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        "VirtualCorpusControllerTest", "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_JSON)
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
        ClientResponse response =
                resource().path("vc").path("access").path("list").path("byGroup")
                        .queryParam("groupId", "2")
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        "dory", "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_JSON)
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
 
    // share VC
    @Test
    public void testCreateDeleteAccess () {
        // TODO Auto-generated method stub

    }
    
    
}
