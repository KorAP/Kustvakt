package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MediaType;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author hanl, margaretha
 * @lastUpdate 18/03/2019
 *
 */
public class SearchControllerTest extends SpringJerseyTest {

    @Autowired
    private KustvaktConfiguration config;

    private JsonNode requestSearchWithFields(String fields) throws KustvaktException{
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("fields", fields)
                .queryParam("context", "sentence").queryParam("count", "13")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        return node;
    }
    
    private String createJsonQuery(){
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=der]", "poliqarp");
        s.setCollection("corpusSigle=GOE");
        s.setQuery("Wasser", "poliqarp");
        return s.toJSON();
    }

    @Test
    public void testApiWelcomeMessage () {
        ClientResponse response = resource().path(API_VERSION).path("")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String message = response.getEntity(String.class);
        assertEquals(message, config.getApiWelcomeMessage());
    }

    @Test
    public void testSearchWithField () throws KustvaktException {
        JsonNode node = requestSearchWithFields("author");
        assertNotEquals(0, node.at("/matches").size());
        assertEquals("[\"author\"]",
                node.at("/meta/fields").toString());
    }
    
    @Test
    public void testSearchWithMultipleFields () throws KustvaktException {
        JsonNode node = requestSearchWithFields("author, title");
        assertNotEquals(0, node.at("/matches").size());
        assertEquals("[\"author\",\"title\"]",
                node.at("/meta/fields").toString());
    }
    
    @Test
    public void testSearchQueryPublicCorpora () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("availability", node.at("/collection/key").asText());
        assertEquals("CC-BY.*", node.at("/collection/value").asText());
        assertEquals("availability(FREE)",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
    }

    @Test
    public void testSearchQueryFailure () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der").queryParam("ql", "poliqarp")
                .queryParam("cq", "corpusSigle=WPD | corpusSigle=GOE")
                .queryParam("count", "13").accept(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(302, node.at("/errors/0/0").asInt());
        assertEquals(302, node.at("/errors/1/0").asInt());
        assertTrue(node.at("/errors/2").isMissingNode());
        assertFalse(node.at("/collection").isMissingNode());
        assertEquals(13, node.at("/meta/count").asInt());
    }

    @Test
    public void testSearchQueryWithMeta () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cutoff", "true").queryParam("count", "5")
                .queryParam("page", "1").queryParam("context", "40-t,30-t")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.at("/meta/cutOff").asBoolean());
        assertEquals(5, node.at("/meta/count").asInt());
        assertEquals(0, node.at("/meta/startIndex").asInt());
        assertEquals("token", node.at("/meta/context/left/0").asText());
        assertEquals(40, node.at("/meta/context/left/1").asInt());
        assertEquals(30, node.at("/meta/context/right/1").asInt());
        assertEquals(-1, node.at("/meta/totalResults").asInt());
    }

    @Test
    public void testSearchQueryFreeExtern () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("availability", node.at("/collection/key").asText());
        assertEquals("CC-BY.*", node.at("/collection/value").asText());
        assertEquals("availability(FREE)",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
    }

    @Test
    public void testSearchQueryFreeIntern () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .header(HttpHeaders.X_FORWARDED_FOR, "172.27.0.32")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("availability", node.at("/collection/key").asText());
        assertEquals("CC-BY.*", node.at("/collection/value").asText());
        assertEquals("availability(FREE)",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
    }

    @Test
    public void testSearchQueryExternAuthorized () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        // System.out.println(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("ACA.*",
                node.at("/collection/operands/1/operands/0/value").asText());
        assertEquals("QAO-NC",
                node.at("/collection/operands/1/operands/1/value").asText());
        assertEquals("operation:or", node.at("/collection/operation").asText());
        assertEquals("availability(PUB)",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
    }

    @Test
    public void testSearchQueryInternAuthorized () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "172.27.0.32")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
//        System.out.println(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("ACA.*",
                node.at("/collection/operands/1/operands/0/value").asText());
        assertEquals("QAO-NC",
                node.at("/collection/operands/1/operands/1/operands/0/value")
                        .asText());
        assertEquals("QAO.*",
                node.at("/collection/operands/1/operands/1/operands/1/value")
                        .asText());
        assertEquals("operation:or", node.at("/collection/operation").asText());
        assertEquals("availability(ALL)",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
    }

    @Test
    public void testSearchQueryWithCollectionQueryAuthorizedWithoutIP ()
            throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("cq", "textClass=politik & corpusSigle=BRZ10")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotNull(node);
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
        assertEquals("availability(FREE)",
                node.at("/collection/rewrites/0/scope").asText());
        // EM: double AND operations
        assertEquals("availability",
                node.at("/collection/operands/0/key").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("textClass",
                node.at("/collection/operands/1/operands/0/key").asText());
        assertEquals("corpusSigle",
                node.at("/collection/operands/1/operands/1/key").asText());
    }

    @Test
    @Ignore
    public void testSearchQueryAuthorizedWithoutIP () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("ACA.*", node.at("/collection/operands/1/value").asText());
        assertEquals("operation:or", node.at("/collection/operation").asText());
        assertEquals("availability(PUB)",
                node.at("/collection/rewrites/0/scope").asText());
    }
    
    @Test
    public void testSearchWithInvalidPage () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .queryParam("page", "0")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
        assertEquals("page must start from 1",node.at("/errors/0/1").asText());
    }

    @Test
    public void testSearchSentenceMeta () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("context", "sentence").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("base/s:s", node.at("/meta/context").asText());
        assertNotEquals("${project.version}", "/meta/version");
    }

//  EM: The API is disabled
    @Ignore
    @Test
    public void testSearchSimpleCQL () throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("(der) or (das)", "CQL");

        ClientResponse response = resource().path(API_VERSION).path("search")
                .post(ClientResponse.class, s.toJSON());
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        // assertEquals(17027, node.at("/meta/totalResults").asInt());
    }

//  EM: The API is disabled
    @Test
    @Ignore
    public void testSearchRawQuery () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .post(ClientResponse.class, createJsonQuery());
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        
        assertEquals("availability(FREE)",
                node.at("/collection/rewrites/0/scope").asText());
    }
    
//  EM: The API is disabled    
    @Test
    @Ignore
    public void testSearchPostAll () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .header(HttpHeaders.X_FORWARDED_FOR, "10.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .post(ClientResponse.class, createJsonQuery());
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        
        assertEquals("availability(ALL)",
                node.at("/collection/rewrites/0/scope").asText());
    }
    
//  EM: The API is disabled
    @Test
    @Ignore
    public void testSearchPostPublic () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .post(ClientResponse.class, createJsonQuery());
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        
        assertEquals("availability(PUB)",
                node.at("/collection/rewrites/0/scope").asText());
    }
}
