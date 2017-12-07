package de.ids_mannheim.korap.web.service.full;

/**
 * @author hanl, margaretha
 * @lastUpdate 19/04/2017
 * EM: FIX ME: Database restructure
 */
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/* EM: potentially an unused service! */

@Ignore
public class QuerySerializationServiceTest extends FastJerseyTest {

    @Autowired
    HttpAuthorizationHandler handler;
    
    @Override
    public void initMethod () throws KustvaktException {
        //        helper().runBootInterfaces();
    }


    @Test
    public void testQuerySerializationFilteredPublic () throws KustvaktException {
        ClientResponse response = resource()

                .path("corpus/WPD13/query").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("corpusSigle", node.at("/collection/key").asText());
        assertEquals("WPD13", node.at("/collection/value").asText());
    }



    @Test
    public void testQuerySerializationUnexistingResource () throws KustvaktException {
        ClientResponse response = resource()

                .path("corpus/ZUW19/query").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals("[Cannot found public Corpus with ids: [ZUW19]]",
                node.at("/errors/0/2").asText());
    }


    @Test
    public void testQuerySerializationWithNonPublicCorpus () throws KustvaktException {
        ClientResponse response = resource()

                .path("corpus/BRZ10/query").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(101, node.at("/errors/0/0").asInt());
        assertEquals("[Cannot found public Corpus with ids: [BRZ10]]",
                node.at("/errors/0/2").asText());
    }


    @Test
    public void testQuerySerializationWithAuthentication () throws KustvaktException {
        ClientResponse response = resource()

                .path("corpus/BRZ10/query").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("corpusSigle", node.at("/collection/key").asText());
        assertEquals("BRZ10", node.at("/collection/value").asText());
    }


    @Test
    public void testQuerySerializationWithNewCollection () throws KustvaktException {
        // Add Virtual Collection
        ClientResponse response = resource()

                .path("virtualcollection").queryParam("filter", "false")
                .queryParam("query",
                        "creationDate since 1775 & corpusSigle=GOE")
                .queryParam("name", "Weimarer Werke")
                .queryParam("description", "Goethe-Werke in Weimar (seit 1775)")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals("Weimarer Werke", node.path("name").asText());

        // Get virtual collections
        response = resource()

                .path("collection")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        ent = response.getEntity(String.class);
        node = JsonUtils.readTree(ent);
        assertNotNull(node);

        Iterator<JsonNode> it = node.elements();
        String id = null;
        while (it.hasNext()) {
            JsonNode next = (JsonNode) it.next();
            if ("Weimarer Werke".equals(next.path("name").asText()))
                id = next.path("id").asText();
        }
        assertNotNull(id);
        assertFalse(id.isEmpty());

        // query serialization service
        response = resource()

                .path("collection").path(id).path("query")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("context", "base/s:s")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        ent = response.getEntity(String.class);
        node = JsonUtils.readTree(ent);
        assertNotNull(node);
        System.out.println("NODE " + ent);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("koral:doc",
                node.at("/collection/operands/0/@type").asText());
        assertEquals("creationDate",
                node.at("/collection/operands/0/key").asText());
        assertEquals("1775", node.at("/collection/operands/0/value").asText());
        assertEquals("type:date",
                node.at("/collection/operands/0/type").asText());
        assertEquals("match:geq",
                node.at("/collection/operands/0/match").asText());

        assertEquals("koral:doc",
                node.at("/collection/operands/1/@type").asText());
        assertEquals("corpusSigle",
                node.at("/collection/operands/1/key").asText());
        assertEquals("GOE", node.at("/collection/operands/1/value").asText());
        assertEquals("match:eq",
                node.at("/collection/operands/1/match").asText());
    }


    @Test
    public void testQuerySerializationOfVirtualCollection () throws KustvaktException {
        ClientResponse response = resource()

                .path("collection/GOE-VC/query").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:doc",
                node.at("/collection/operands/0/@type").asText());
        assertEquals("corpusSigle",
                node.at("/collection/operands/0/key").asText());
        assertEquals("GOE", node.at("/collection/operands/0/value").asText());
        assertEquals("koral:doc",
                node.at("/collection/operands/1/@type").asText());
        assertEquals("creationDate",
                node.at("/collection/operands/1/key").asText());
        assertEquals("1810-01-01",
                node.at("/collection/operands/1/value").asText());

    }


    @Test
    public void testMetaQuerySerialization () throws KustvaktException {
        ClientResponse response = resource()

                .path("query").queryParam("context", "sentence")
                .queryParam("count", "20").queryParam("page", "5")
                .queryParam("cutoff", "true").queryParam("q", "[pos=ADJA]")
                .queryParam("ql", "poliqarp")
                .method("GET", ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);

        assertEquals("sentence", node.at("/meta/context").asText());
        assertEquals(20, node.at("/meta/count").asInt());
        assertEquals(5, node.at("/meta/startPage").asInt());
        assertEquals(true, node.at("/meta/cutOff").asBoolean());

        assertEquals("koral:term", node.at("/query/wrap/@type").asText());
        assertEquals("pos", node.at("/query/wrap/layer").asText());
        assertEquals("match:eq", node.at("/query/wrap/match").asText());
        assertEquals("ADJA", node.at("/query/wrap/key").asText());
    }


    @Test
    public void testMetaQuerySerializationWithOffset () throws KustvaktException{
        ClientResponse response = resource()

                .path("query").queryParam("context", "sentence")
                .queryParam("count", "20").queryParam("page", "5")
                .queryParam("offset", "2").queryParam("cutoff", "true")
                .queryParam("q", "[pos=ADJA]").queryParam("ql", "poliqarp")
                .method("GET", ClientResponse.class);
        assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode());

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);

        assertEquals("sentence", node.at("/meta/context").asText());
        assertEquals(20, node.at("/meta/count").asInt());
        assertEquals(2, node.at("/meta/startIndex").asInt());
        assertEquals(true, node.at("/meta/cutOff").asBoolean());
    }

}
