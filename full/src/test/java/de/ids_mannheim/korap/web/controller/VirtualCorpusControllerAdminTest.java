package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

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
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.api.client.UniformInterfaceException;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VirtualCorpusControllerAdminTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;

    private String admin = "admin";
    private String username = "VirtualCorpusControllerAdminTest";

    @Test
    public void testSearchPrivateVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("vc").path("1")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(admin,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(1, node.at("/id").asInt());
        assertEquals("dory VC", node.at("/name").asText());
    }

    @Test
    public void testSearchProjectVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        ClientResponse response = resource().path("vc").path("2")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(admin,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("group VC", node.at("/name").asText());
        assertEquals(VirtualCorpusType.PROJECT.displayName(),
                node.at("/type").asText());
    }

    @Test
    public void testListDoryVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("vc").path("list")
                .queryParam("createdBy", "dory")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, handler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(4, node.size());
    }

    private JsonNode testListSystemVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("vc").path("list")
                .path("system-admin").queryParam("type", "SYSTEM")
                .queryParam("createdBy", admin)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, handler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    @Test
    public void testCreateSystemVC () throws KustvaktException {
        String json = "{\"name\": \"new system vc\",\"type\": \"SYSTEM\","
                + "\"corpusQuery\": \"creationDate since 1820\"}";

        ClientResponse response = resource().path("vc").path("create")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(admin,
                                "pass"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = testListSystemVC();
        assertEquals(1, node.size());

        String vcId = node.at("/0/id").asText();

        testDeleteSystemVC(vcId);
    }

    private void testDeleteSystemVC (String vcId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response =
                resource().path("vc").path("delete").path(vcId)
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        admin, "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = testListSystemVC();
        assertEquals(0, node.size());
    }

    @Test
    public void testPrivateVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        String json = "{\"name\": \"new vc\",\"type\": \"PRIVATE\","
                + "\"corpusQuery\": \"corpusSigle=GOE\"}";

        ClientResponse response = resource().path("vc").path("create")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(username,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = testListUserVC();
        assertEquals(1, node.size());

        String vcId = node.at("/0/id").asText();
        testEditPrivateVC(vcId);
        testDeletePrivateVC(vcId);
    }

    private JsonNode testListUserVC () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path("vc").path("list")
                .path("system-admin").queryParam("createdBy", username)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION, handler
                        .createBasicAuthorizationHeaderValue(admin, "pass"))
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        return JsonUtils.readTree(entity);
    }

    private void testEditPrivateVC (String vcId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        String json = "{\"id\": \"" + vcId + "\", \"name\": \"edited vc\"}";

        ClientResponse response = resource().path("vc").path("edit")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(admin,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .post(ClientResponse.class, json);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = testListUserVC();
        assertEquals("edited vc", node.at("/0/name").asText());
    }

    private void testDeletePrivateVC (String vcId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        ClientResponse response =
                resource().path("vc").path("delete").path(vcId)
                        .header(Attributes.AUTHORIZATION,
                                handler.createBasicAuthorizationHeaderValue(
                                        admin, "pass"))
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = testListUserVC();
        assertEquals(0, node.size());
    }


    private String testlistAccessByVC (String vcId) throws KustvaktException {
        ClientResponse response = resource().path("vc").path("access")
                .path("list").queryParam("vcId", vcId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(admin,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(1, node.size());
        node = node.get(0);

        assertEquals(admin, node.at("/createdBy").asText());
        assertEquals(5, node.at("/vcId").asInt());
        assertEquals("marlin VC", node.at("/vcName").asText());
        assertEquals(1, node.at("/userGroupId").asInt());
        assertEquals("marlin group", node.at("/userGroupName").asText());

        return node.at("/accessId").asText();
    }

    private void testlistAccessByGroup (String groupId)
            throws KustvaktException {
        ClientResponse response = resource().path("vc").path("access")
                .path("list").path("byGroup").queryParam("groupId", groupId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(admin,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
    }

    @Test
    public void testVCSharing () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        String vcId = "5";
        String groupId = "1";

        testCreateVCAccess(vcId, groupId);
        testlistAccessByGroup(groupId);

        String accessId = testlistAccessByVC(vcId);
        testDeleteVCAccess(accessId);
    }

    private void testCreateVCAccess (String vcId, String groupId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        // marlin vc
        form.add("vcId", vcId);
        // marlin group
        form.add("groupId", groupId);

        ClientResponse response;
        // share VC
        response = resource().path("vc").path("access").path("share")
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue(admin,
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").entity(form)
                .post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

    }

    private void testDeleteVCAccess (String accessId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        ClientResponse response = resource().path("vc").path("access")
                .path("delete").path(accessId)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("dory",
                                "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

    }
}
