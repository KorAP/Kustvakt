package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/**
 * @author hanl, margaretha
 * @lastUpdate 21/04/2017
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
    public void testSearchQueryAuthorized () {
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
    public void testSearchQueryWithCollectionQueryAuthorized () {
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
    public void testSearchForPublicCorpusWithStringId () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("GOE").path("search")
                .queryParam("q", "blau").queryParam("ql", "poliqarp")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("corpusSigle", node.at("/collection/key").asText());
        assertEquals("GOE", node.at("/collection/value").asText());
        assertNotEquals(0, node.path("matches").size());
        assertEquals(32, node.at("/meta/totalResults").asInt());
    }
    
    @Test
    public void testSearchForVirtualCollectionWithStringId () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection").path("GOE-VC").path("search")
                .queryParam("q", "blau").queryParam("ql", "poliqarp")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        System.out.println(ent);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and", node.at("/collection/operation").asText());
        assertNotEquals(0, node.at("/collection/operands").size());
        assertEquals("corpusSigle",
                node.at("/collection/operands/0/key").asText());
        assertEquals("GOE",
                node.at("/collection/operands/0/value").asText());
        assertEquals("creationDate",
                node.at("/collection/operands/1/key").asText());
        assertEquals("1810-01-01",
                node.at("/collection/operands/1/value").asText());
        assertEquals(1, node.at("/meta/totalResults").asInt());
    }

    
    @Test
    public void testSearchForPublicCorpusWithIntegerId () throws KustvaktException {
        Set<Corpus> publicCorpora = ResourceFinder.searchPublic(Corpus.class);
        Iterator<Corpus> i = publicCorpora.iterator();
        String id = null;
        while (i.hasNext()){
            Corpus c = i.next();
            if (c.getName().equals("Goethe")){
                id =c.getId().toString();
            }
        }
            
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path(id).path("search")
                .queryParam("q", "blau").queryParam("ql", "poliqarp")
                .get(ClientResponse.class);
        
        String ent = response.getEntity(String.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("corpusSigle", node.at("/collection/key").asText());
        assertEquals("GOE", node.at("/collection/value").asText());
        assertNotEquals(0, node.path("matches").size());
    }
    
    @Test
    public void testSearchForCorpusWithStringIdUnauthorized () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("WPD15").path("search")
                .queryParam("q", "blau").queryParam("ql", "poliqarp")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode error = JsonUtils.readTree(ent).get("errors").get(0);
        assertEquals(101, error.get(0).asInt());
        assertEquals("[Cannot found public resources with ids: [WPD15]]",
                error.get(2).asText());
    }
    
    @Test
    public void testSearchForOwnersCorpusWithStringId () {
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path("WPD15").path("search")
                .queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("corpusSigle", node.at("/collection/key").asText());
        assertEquals("WPD15", node.at("/collection/value").asText());
        assertNotEquals(0, node.path("matches").size());
    }
    
    @Test
    public void testSearchForOwnersCorpusWithIntegerId () throws KustvaktException {
        Set<Corpus> publicCorpora = ResourceFinder.searchPublic(Corpus.class);
        Iterator<Corpus> i = publicCorpora.iterator();
        String id = null;
        while (i.hasNext()){
            Corpus c = i.next();
            if (c.getPersistentID().equals("WPD15")){
                id =c.getId().toString();
                System.out.println(id);
            }
        }
        ClientResponse response = resource().path(getAPIVersion())
                .path("corpus").path(id).path("search")
                .queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("corpusSigle", node.at("/collection/key").asText());
        assertEquals("WPD15", node.at("/collection/value").asText());
        assertNotEquals(0, node.path("matches").size());
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
