package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.ResourceDao;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/**
 * @author hanl
 * @date 14/01/2016
 */
public class ResourceServiceTest extends FastJerseyTest {
    
    @BeforeClass
    public static void configure () throws Exception {
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }

    // create a simple test collection for user kustvakt, otherwise test fails
    @Test
    @Ignore
    public void testStats () {
        ClientResponse response = resource()
                .path(getAPIVersion())
                .path("collection")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotNull(node);
        assertNotEquals(0, node.size());
        String id = node.path(1).path("id").asText();

        response = resource()
                .path(getAPIVersion())
                .path("collection")
                .path(id)
                .path("stats")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
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

    // EM: The test covers multiple operations because it deals with 
    // the same resource and needs an order to operate (store followed by
    // update followed by delete).
    @Test
    public void testVirtualCollectionStoreUpdateDelete() throws KustvaktException, 
        JsonProcessingException, IOException {
    	// resource store service
        ClientResponse response = resource()
                .path(getAPIVersion())
                .path("virtualcollection")
                .queryParam("filter", "false")
                .queryParam("name", "Goethe")
                .queryParam("description", "Goethe corpus")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
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
        ResourceDao<?> dao = new ResourceDao<>(helper().getContext()
                .getPersistenceClient());
        assertEquals("sqlite", helper().getContext().getPersistenceClient()
                .getDatabase());

        assertNotEquals(0, dao.size());
        KustvaktResource res = dao.findbyId(id,
                User.UserFactory.getDemoUser());
        assertNotNull(res);
        Assert.assertEquals("Goethe",res.getName().toString());
        
        // no update resource service
        response = resource()
                .path(getAPIVersion())
                .path("virtualcollection")
                .path(id)
                .queryParam("name", "Goethe")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);
        
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(
                "[No change has found.]",
                node.get("errors").get(0).get(2).asText());
        
        // update resource service
        response = resource()
                .path(getAPIVersion())
                .path("virtualcollection")
                .path(id)
                .queryParam("name", "Goethe collection")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        
        res = dao.findbyId(id,
                User.UserFactory.getDemoUser());
        assertNotNull(res);
        Assert.assertEquals("Goethe collection",res.getName().toString());
        
        
        // delete resource service
    	response = resource()
                .path(getAPIVersion())
                .path("virtualcollection")
                .path(id)
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .delete(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        
        // check if the resource is *not* in the db anymore
        dao = new ResourceDao<>(helper().getContext()
                .getPersistenceClient());
        assertEquals("sqlite", helper().getContext().getPersistenceClient()
                .getDatabase());

        res = dao.findbyId(id,
                User.UserFactory.getDemoUser());
        assertEquals(null,res);
    }

    @Override
    public void initMethod () throws KustvaktException {
        helper().runBootInterfaces();
    }
}
