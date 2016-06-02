package de.ids_mannheim.korap.web.service.full;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
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
    public void testSearchSimpleAuthorized () {
        ClientResponse response = resource()
                .path(getAPIVersion())
                .path("search")
                .queryParam("q", "[orth=Haus]")
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
    public void testSearchSimpleWithCQAuthorized () {
        ClientResponse response = resource()
                .path(getAPIVersion())
                .path("search")
                .queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp")
                .queryParam("cq", "textClass=politik && corpusSigle=WPD")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotNull(node);

    }


    @Test
    public void testSearchSimpleDemo () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[base=Haus]")
                .queryParam("ql", "poliqarp").get(ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        System.out.println("NODE " + node);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
    }


    @Test
    public void testSearchSentenceMeta () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[base=Haus]")
                .queryParam("ql", "poliqarp").queryParam("context", "sentence")
                .get(ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals("base/s:s", node.at("/meta/context").asText());
        assertNotEquals("${project.version}", "/meta/version");
    }


    @Test
    public void testSearchSimpleCQL () {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");

        ClientResponse response = resource().path(getAPIVersion())
                .path("search").post(ClientResponse.class, s.toJSON());
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
    }


    @Test
    public void testSearchRawQuery () {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        //        s.setCollection("corpusSigle=WPD");

        ClientResponse response = resource().path(getAPIVersion())
                .path("search").post(ClientResponse.class, s.toJSON());
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);

        System.out.println(ent);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
    }


    @Test
    public void testCollectionsGetPublic () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotNull(node);
        assertNotEquals(0, node.size());
    }


    @Test
    public void testCollectionsGet () {
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
    }


    @Test
    public void testCorporaGet () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").get(ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
    }


    @Test
    public void testFoundriesGet () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("foundry").get(ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
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
    public void testCollecionGet () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection").path("id").get(ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());

    }


    @Test
    public void testCorpusGet () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("id").get(ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
    }


    @Test
    public void testFoundryGet () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("foundry").path("id").get(ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.size());
    }


    @Test
    public void testResourceStore () {

    }


    @Test
    public void testResourceDelete () {

    }


    @Test
    public void testSerializationQueryWithCorpusUnAuthorized () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus/WPD/search").queryParam("q", "[base=Haus]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .method("TRACE", ClientResponse.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(401, node.at("/errors/0/0").asInt());
    }


    @Test
    public void testSerializationQueryWithCorpus () {
        ClientResponse response = resource()
                .path(getAPIVersion())
                .path("corpus/WPD/search")
                .queryParam("q", "[base=Haus]")
                .queryParam("ql", "poliqarp")
                .queryParam("context", "base/s:s")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .method("TRACE", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
    }


    @Test
    public void testSerializationQueryWithCollection () {
        ClientResponse response = resource()
                .path(getAPIVersion())
                .path("collection")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);

        String id = node.at("/0/id").asText();
        assertNotNull(id);
        assertFalse(id.isEmpty());

        response = resource()
                .path(getAPIVersion())
                .path("collection")
                .path(id)
                .path("search")
                .queryParam("q", "[base=Haus]")
                .queryParam("ql", "poliqarp")
                .queryParam("context", "base/s:s")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .method("TRACE", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        ent = response.getEntity(String.class);
        node = JsonUtils.readTree(ent);
        assertNotNull(node);
        System.out.println(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());

    }


    @Test
    public void testSerializationQueryPublicCorpora () {
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


    // use trace for this
    @Test
    @Ignore
    public void testSerializationMeta () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("context", "sentence")
                .queryParam("q", "[pos=ADJA]").queryParam("ql", "poliqarp")

                .method("TRACE", ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());
    }


    @Test
    public void testMatchInfoGet () {

    }


    @Test
    public void testMatchInfoSave () {

    }


    @Test
    public void testMatchInfoDelete () {

    }


    @Test
    public void testGetMatches () {

    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().runBootInterfaces();
    }
}
