package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.ResourceDao;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/**
 * @author hanl, margaretha
 * @date 14/01/2016
 * @update 24/04/2017
 */
@Ignore
@Deprecated
public class ResourceServiceTest extends FastJerseyTest {

    @Autowired
    HttpAuthorizationHandler handler;
    
    // create a simple test collection for user kustvakt, otherwise test fails
    @Test
    @Ignore
    public void testStats () throws KustvaktException{
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotNull(node);
        assertNotEquals(0, node.size());
        String id = node.path(1).path("id").asText();

        response = resource().path(getAPIVersion()).path("collection").path(id)
                .path("stats")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotNull(node);
        assertNotNull(node);
        int docs = node.path("documents").asInt();
        assertNotEquals(0, docs);
        assertTrue(docs < 15);
    }


    @Test
    public void testOwnerUpdateVirtualCollection () throws KustvaktException {
        ClientResponse response = resource().path(getAPIVersion())
                .path("virtualcollection").path("GOE-VC") // persistent id
                .queryParam("name", "Goethe collection")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        ResourceDao<?> dao = new ResourceDao<>(
                helper().getContext().getPersistenceClient());
        assertEquals("sqlite",
                helper().getContext().getPersistenceClient().getDatabase());

        KustvaktResource res = dao.findbyId("GOE-VC",
                User.UserFactory.getDemoUser());
        assertNotNull(res);
        assertEquals("Goethe collection", res.getName().toString());

    }


    @Test
    public void testOwnerUpdateCorpus () throws KustvaktException {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("GOE") // persistent id
                .queryParam("name", "Goethe corpus")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        ResourceDao<?> dao = new ResourceDao<>(
                helper().getContext().getPersistenceClient());

        KustvaktResource res = dao.findbyId("GOE",
                User.UserFactory.getDemoUser());
        assertNotNull(res);
        assertEquals("Goethe corpus", res.getName().toString());

    }


    @Test
    public void testOwnerUpdateFoundry () throws KustvaktException {
        ClientResponse response = resource().path(getAPIVersion())
                .path("foundry").path("malt") // persistent id
                .queryParam("name", "malt parser")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        ResourceDao<?> dao = new ResourceDao<>(
                helper().getContext().getPersistenceClient());

        KustvaktResource res = dao.findbyId("malt",
                User.UserFactory.getDemoUser());
        assertNotNull(res);
        assertEquals("malt parser", res.getName().toString());

    }


    @Test
    public void testOwnerUpdateLayer () throws KustvaktException {
        ClientResponse response = resource().path(getAPIVersion()).path("layer")
                .path("mate/d").queryParam("name", "Mate dependency")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        ResourceDao<?> dao = new ResourceDao<>(
                helper().getContext().getPersistenceClient());

        KustvaktResource res = dao.findbyId("mate/d",
                User.UserFactory.getDemoUser());
        assertNotNull(res);
        assertEquals("Mate dependency", res.getName().toString());

    }


    @Test
    public void testOwnerUpdateUnexistingCorpus () throws KustvaktException {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("GOEC") // persistent id
                .queryParam("name", "Goethe corpus")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals("Resource not found!",
                node.get("errors").get(0).get(1).asText());

    }


    @Test
    public void testUpdateUnauthorized () throws KustvaktException {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("GOE") // persistent id
                .queryParam("name", "Goethe corpus").post(ClientResponse.class);

        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals("Permission denied for resource id GOE for the user.",
                node.get("errors").get(0).get(1).asText());

    }


    @Test
    public void testStoreNewVirtualCollection () throws KustvaktException {
        ClientResponse response = resource().path(getAPIVersion())
                .path("virtualcollection").queryParam("filter", "false")
                .queryParam("name", "Brown")
                .queryParam("description", "Brown corpus")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals("Brown", node.path("name").asText());
        assertEquals("Brown corpus", node.path("description").asText());

        String id = node.path("id").asText();

        // check if the resource is in the db
        ResourceDao<?> dao = new ResourceDao<>(
                helper().getContext().getPersistenceClient());
        assertEquals("sqlite",
                helper().getContext().getPersistenceClient().getDatabase());

        assertNotEquals(0, dao.size());
        KustvaktResource res = dao.findbyId(id, User.UserFactory.getDemoUser());
        assertEquals("Brown", res.getName().toString());

    }


    @Test
    public void testStoreUnsupportedType () throws KustvaktException {

        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").queryParam("filter", "false")
                .queryParam("name", "Brown")
                .queryParam("description", "Brown corpus")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        String ent = response.getEntity(String.class);
//        System.out.println(ent);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(402, node.at("/errors/0/0").asInt());
        assertEquals("Unsupported operation for the given resource type.",
                node.at("/errors/0/1").asText());
    }


    @Test
    public void testStoreNewVirtualCollectionFromExistingCollection ()
            throws KustvaktException {
        ClientResponse response = resource().path(getAPIVersion())
                .path("virtualcollection").queryParam("filter", "true")
                .queryParam("ref", "WPD15-VC")
                .queryParam("name", "Wikipedia collection")
                .queryParam("query", "author ~ Asdert")
                .queryParam("description", "Wikipedia subcorpus from Asdert")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        String id = node.path("id").asText();

        assertEquals("Wikipedia collection", node.path("name").asText());
        assertEquals("Wikipedia subcorpus from Asdert",
                node.path("description").asText());
        node = node.at("/data/collection/operands/1");
        assertEquals("author", node.at("/key").asText());
        assertEquals("Asdert", node.at("/value").asText());

        // check if the resource is in the db
        ResourceDao<?> dao = new ResourceDao<>(
                helper().getContext().getPersistenceClient());
        KustvaktResource res = dao.findbyId(id, User.UserFactory.getDemoUser());
        assertEquals("Wikipedia collection", res.getName().toString());
    }


    // EM: The test covers multiple ordered operations dealing with 
    // the same resource (store followed by update followed by delete).
    @Test
    public void testVirtualCollectionStoreUpdateDelete ()
            throws KustvaktException, JsonProcessingException, IOException {
        // resource store service
        ClientResponse response = resource().path(getAPIVersion())
                .path("virtualcollection").queryParam("filter", "false")
                .queryParam("name", "Goethe")
                .queryParam("description", "Goethe corpus")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals("Goethe", node.path("name").asText());
        assertEquals("Goethe corpus", node.path("description").asText());

        String id = node.path("id").asText();

        // check if the resource is in the db
        ResourceDao<?> dao = new ResourceDao<>(
                helper().getContext().getPersistenceClient());
        assertEquals("sqlite",
                helper().getContext().getPersistenceClient().getDatabase());

        assertNotEquals(0, dao.size());
        KustvaktResource res = dao.findbyId(id, User.UserFactory.getDemoUser());
        assertNotNull(res);
        Assert.assertEquals("Goethe", res.getName().toString());

        // no update resource service
        response = resource().path(getAPIVersion()).path("virtualcollection")
                .path(id).queryParam("name", "Goethe")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals("[No change has found.]",
                node.get("errors").get(0).get(2).asText());

        // update resource service
        response = resource().path(getAPIVersion()).path("virtualcollection")
                .path(id).queryParam("name", "Goethe collection")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        res = dao.findbyId(id, User.UserFactory.getDemoUser());
        assertNotNull(res);
        Assert.assertEquals("Goethe collection", res.getName().toString());


        // delete resource service
        response = resource().path(getAPIVersion()).path("virtualcollection")
                .path(id)
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .delete(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        // check if the resource is *not* in the db anymore
        dao = new ResourceDao<>(helper().getContext().getPersistenceClient());
        assertEquals("sqlite",
                helper().getContext().getPersistenceClient().getDatabase());

        res = dao.findbyId(id, User.UserFactory.getDemoUser());
        assertEquals(null, res);
    }


    @Override
    public void initMethod () throws KustvaktException {
//        helper().runBootInterfaces();
    }
}
