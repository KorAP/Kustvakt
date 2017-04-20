package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/**
 * @author hanl, margaretha
 * @lastUpdate 19/04/2017
 *
 */
public class SearchServiceTest extends FastJerseyTest {

    @Override
    public void initMethod () throws KustvaktException {
        helper().runBootInterfaces();
    }


    @BeforeClass
    public static void configure () throws Exception {
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }


    @Test
    public void testSearchQueryPublicCorpora () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:or", node.at("/collection/operation").asText());
        assertNotEquals(0, node.at("/collection/operands").size());
        assertEquals("corpusSigle([GOE, WPD13])",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals(6218, node.at("/meta/totalResults").asInt());
    }


    @Test
    public void testSearchSimpleAuthorized () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[orth=die]")
                .queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals("corpusSigle([GOE, WPD13, WPD15, BRZ10])",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals(7665, node.at("/meta/totalResults").asInt());
    }


    @Test
    public void testSearchSimpleWithCQAuthorized () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp")
                .queryParam("cq", "textClass=politik & corpusSigle=BRZ10")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotNull(node);
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals("textClass",
                node.at("/collection/operands/0/key").asText());
        assertEquals("corpusSigle",
                node.at("/collection/operands/1/key").asText());
        assertEquals("koral:token", node.at("/query/@type").asText());
    }


    @Test
    public void testSearchSimpleDemo () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals("corpusSigle([GOE, WPD13])",
                node.at("/collection/rewrites/0/scope").asText());
    }


    @Test
    @Ignore
    public void testSearchPublicCorpusWithID () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("GOE").path("search")
                .queryParam("q", "blau").queryParam("ql", "poliqarp")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:or", node.at("/collection/operation").asText());
        assertNotEquals(0, node.at("/collection/operands").size());
        assertEquals("corpusSigle([GOE])",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals(6218, node.at("/meta/totalResults").asInt());
    }


    @Test
    public void testSearchSentenceMeta () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "sentence")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
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
        s.setQuery("(der) or (das)", "CQL");

        ClientResponse response = resource().path(getAPIVersion())
                .path("search").post(ClientResponse.class, s.toJSON());
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        //        assertEquals(17027, node.at("/meta/totalResults").asInt());
    }


    @Test
    public void testSearchRawQuery () {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=der]", "poliqarp");
        s.setCollection("corpusSigle=GOE");

        ClientResponse response = resource().path(getAPIVersion())
                .path("search").post(ClientResponse.class, s.toJSON());
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);


        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        //        assertEquals(10993, node.at("/meta/totalResults").asInt());
    }

}
