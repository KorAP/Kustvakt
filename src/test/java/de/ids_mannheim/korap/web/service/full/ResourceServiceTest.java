package de.ids_mannheim.korap.web.service.full;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

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


    @Test
    public void testSearchSimple () {
        ClientResponse response = resource()
                .path(getAPIVersion())
                .path("search")
                .queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
    }


    @Test
    public void testCollectionGet () {
        ClientResponse response = resource()
                .path(getAPIVersion())
                .path("collection")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotEquals(0, node.size());
    }


    @Test
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

        String id = node.path(0).path("id").asText();

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
        int docs = node.path("documents").asInt();
        assertNotEquals(0, docs);
        assertTrue(docs < 15);
    }


    @Test
    public void testResourceStore () {

    }


    @Test
    public void testResourceDelete () {

    }


    @Test
    @Ignore
    public void testSerializationQueryInCollection () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus/WPD/search").queryParam("q", "[base=Haus]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .method("TRACE", ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        System.out.println("RESPONSE 1 " + response);
        String ent = response.getEntity(String.class);
        System.out.println("Entity 1 " + ent);
    }


    @Test
    public void testSerializationQueryPublic () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[base=Haus]")
                .queryParam("ql", "poliqarp").queryParam("context", "sentence")
                .method("TRACE", ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:or", node.at("/collection/operation").asText());
        assertNotEquals(0, node.at("/collection/operands").size());
    }


    @Test
    public void testQuery () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[base=Haus]")
                .queryParam("ql", "poliqarp").queryParam("context", "sentence")
                .get(ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("base/s:s", node.at("/meta/context").asText());
        assertNotEquals("${project.version}", "/meta/version");
    }


    @Test
    @Ignore
    public void testSerializationMeta () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("context", "sentence")
                .queryParam("q", "[pos=ADJA]").queryParam("ql", "poliqarp")
                .get(ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
    }


    @Test
    @Ignore
    public void testSerializationCollection () {
        ClientResponse response = resource().path(getAPIVersion()).path("")
                .get(ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
    }


    @Test
    public void testMatchInfo () {

    }


    @Test
    public void testGetResources () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").get(ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        System.out.println("NODE 1 " + node);
    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().runBootInterfaces();
    }
}
