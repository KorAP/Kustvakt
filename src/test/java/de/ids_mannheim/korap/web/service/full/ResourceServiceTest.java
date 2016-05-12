package de.ids_mannheim.korap.web.service.full;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author hanl
 * @date 14/01/2016
 */
public class ResourceServiceTest extends FastJerseyTest {

    @BeforeClass
    public static void configure() throws Exception {
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }

    @Test
    @Ignore
    public void testSearchSimple() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp")
                //                .queryParam("cq", "corpusID=GOE")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        Assert.assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
    }

    @Test
    @Ignore
    public void testCollectionGet() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection").header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotEquals(0, node.size());
    }

    @Test
    @Ignore
    public void testStats() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection").header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        Assert.assertNotNull(node);

        System.out.println("-------------------------------");
        System.out.println("NODE COLLECTIONS" + node);
        String id = node.path(0).path("id").asText();

        System.out.println("ID IS " + id);
        System.out.println("FROM NODE " + node);
        response = resource().path(getAPIVersion()).path("collection").path(id)
                .path("stats").header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        Assert.assertNotNull(node);
        int docs = node.path("documents").asInt();
        System.out.println("-------------------------------");
        System.out.println("NODE " + node);
        assertNotEquals(0, docs);
        Assert.assertTrue(docs < 15);
    }

    @Test
    public void testResourceStore() {

    }

    @Test
    @Ignore
    public void testSerializationQueryInCollection() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus/WPD/search").queryParam("q", "[base=Haus]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .method("TRACE", ClientResponse.class);
        System.out.println("RESPONSE 1 " + response);
        String ent = response.getEntity(String.class);
        System.out.println("Entity 1 " + ent);
    }

    @Test
    public void testSerializationQueryPublic() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[base=Haus]")
                .queryParam("ql", "poliqarp").queryParam("context", "sentence")
                .method("TRACE", ClientResponse.class);
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        System.out.println("PUBLIC COLLECTION");
        System.out.println(node);
    }

    @Test
    public void testQuery() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[base=Haus]")
                .queryParam("ql", "poliqarp").queryParam("context", "sentence")
                .get(ClientResponse.class);
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("base/s:s", node.at("/meta/context").asText());
        assertNotEquals("${project.version}", "/meta/version");
    }

    @Test
    @Ignore
    public void testSerializationMeta() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("context", "sentence")
                .queryParam("q", "[pos=ADJA]").queryParam("ql", "poliqarp")
                .get(ClientResponse.class);
    }

    @Test
    @Ignore
    public void testSerializationCollection() {
        ClientResponse response = resource().path(getAPIVersion()).path("")
                .get(ClientResponse.class);
    }

    @Override
    public void initMethod() throws KustvaktException {
        helper().runBootInterfaces();
    }
}
