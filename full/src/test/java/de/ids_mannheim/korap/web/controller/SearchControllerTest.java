package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author hanl, margaretha
 * @lastUpdate 18/03/2019
 */
@DisplayName("Search Controller Test")
class SearchControllerTest extends SpringJerseyTest {

    @Autowired
    private KustvaktConfiguration config;

    private JsonNode requestSearchWithFields(String fields) throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=das]").queryParam("ql", "poliqarp").queryParam("fields", fields).queryParam("context", "sentence").queryParam("count", "13").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        return node;
    }

    private String createJsonQuery() {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=der]", "poliqarp");
        s.setCollection("corpusSigle=GOE");
        s.setQuery("Wasser", "poliqarp");
        return s.toJSON();
    }

    @Test
    @DisplayName("Test Api Welcome Message")
    void testApiWelcomeMessage() {
        Response response = target().path(API_VERSION).path("").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(response.getHeaders().getFirst("X-Index-Revision"), "Wes8Bd4h1OypPqbWF5njeQ==");
        String message = response.readEntity(String.class);
        assertEquals(message, config.getApiWelcomeMessage());
    }

    @Test
    @DisplayName("Test Search Show Tokens")
    void testSearchShowTokens() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=die]").queryParam("ql", "poliqarp").queryParam("show-tokens", true).request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(3, node.at("/matches/0/tokens").size());
        assertFalse(node.at("/matches/0/snippet").isMissingNode());
    }

    @Test
    @DisplayName("Test Search Disable Snippet")
    void testSearchDisableSnippet() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=die]").queryParam("ql", "poliqarp").queryParam("show-snippet", false).queryParam("show-tokens", true).request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertTrue(node.at("/matches/0/snippet").isMissingNode());
        assertEquals(3, node.at("/matches/0/tokens").size());
    }

    @Test
    @DisplayName("Test Search With Field")
    void testSearchWithField() throws KustvaktException {
        JsonNode node = requestSearchWithFields("author");
        assertNotEquals(0, node.at("/matches").size());
        assertEquals(node.at("/meta/fields").toString(), "[\"author\"]");
        assertTrue(node.at("/matches/0/tokens").isMissingNode());
    }

    @Test
    @DisplayName("Test Search With Multiple Fields")
    void testSearchWithMultipleFields() throws KustvaktException {
        JsonNode node = requestSearchWithFields("author, title");
        assertNotEquals(0, node.at("/matches").size());
        assertEquals(node.at("/meta/fields").toString(), "[\"author\",\"title\"]");
    }

    @Test
    @DisplayName("Test Search Query Public Corpora")
    void testSearchQueryPublicCorpora() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/key").asText(), "availability");
        assertEquals(node.at("/collection/value").asText(), "CC-BY.*");
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(FREE)");
        assertEquals(node.at("/collection/rewrites/0/operation").asText(), "operation:insertion");
    }

    @Test
    @DisplayName("Test Search Query Failure")
    void testSearchQueryFailure() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der").queryParam("ql", "poliqarp").queryParam("cq", "corpusSigle=WPD | corpusSigle=GOE").queryParam("count", "13").request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(302, node.at("/errors/0/0").asInt());
        assertEquals(302, node.at("/errors/1/0").asInt());
        assertTrue(node.at("/errors/2").isMissingNode());
        assertFalse(node.at("/collection").isMissingNode());
        assertEquals(13, node.at("/meta/count").asInt());
    }

    @Test
    @DisplayName("Test Search Query With Meta")
    void testSearchQueryWithMeta() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=Bachelor]").queryParam("ql", "poliqarp").queryParam("cutoff", "true").queryParam("count", "5").queryParam("page", "1").queryParam("context", "40-t,30-t").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.at("/meta/cutOff").asBoolean());
        assertEquals(5, node.at("/meta/count").asInt());
        assertEquals(0, node.at("/meta/startIndex").asInt());
        assertEquals(node.at("/meta/context/left/0").asText(), "token");
        assertEquals(40, node.at("/meta/context/left/1").asInt());
        assertEquals(30, node.at("/meta/context/right/1").asInt());
        assertEquals(-1, node.at("/meta/totalResults").asInt());
        for (String path : new String[]{"/meta/count", "/meta/startIndex", "/meta/context/left/1", "/meta/context/right/1", "/meta/totalResults", "/meta/itemsPerPage"}) {
            assertTrue(node.at(path).isNumber(), path + " should be a number");
        }
    }

    @Test
    @DisplayName("Test Search Query Free Extern")
    void testSearchQueryFreeExtern() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=die]").queryParam("ql", "poliqarp").request().header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals(node.at("/collection/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/key").asText(), "availability");
        assertEquals(node.at("/collection/value").asText(), "CC-BY.*");
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(FREE)");
        assertEquals(node.at("/collection/rewrites/0/operation").asText(), "operation:insertion");
    }

    @Test
    @DisplayName("Test Search Query Free Intern")
    void testSearchQueryFreeIntern() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=die]").queryParam("ql", "poliqarp").request().header(HttpHeaders.X_FORWARDED_FOR, "172.27.0.32").get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals(node.at("/collection/@type").asText(), "koral:doc");
        assertEquals(node.at("/collection/key").asText(), "availability");
        assertEquals(node.at("/collection/value").asText(), "CC-BY.*");
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(FREE)");
        assertEquals(node.at("/collection/rewrites/0/operation").asText(), "operation:insertion");
    }

    @Test
    @DisplayName("Test Search Query Extern Authorized")
    void testSearchQueryExternAuthorized() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=die]").queryParam("ql", "poliqarp").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015")).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        // System.out.println(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operands/0/value").asText(), "CC-BY.*");
        assertEquals(node.at("/collection/operands/1/operands/0/value").asText(), "ACA.*");
        assertEquals(node.at("/collection/operands/1/operands/1/value").asText(), "QAO-NC");
        assertEquals(node.at("/collection/operation").asText(), "operation:or");
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(PUB)");
        assertEquals(node.at("/collection/rewrites/0/operation").asText(), "operation:insertion");
    }

    @Test
    @DisplayName("Test Search Query Intern Authorized")
    void testSearchQueryInternAuthorized() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=die]").queryParam("ql", "poliqarp").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015")).header(HttpHeaders.X_FORWARDED_FOR, "172.27.0.32").get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        // System.out.println(node);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operands/0/value").asText(), "CC-BY.*");
        assertEquals(node.at("/collection/operands/1/operands/0/value").asText(), "ACA.*");
        assertEquals(node.at("/collection/operands/1/operands/1/operands/0/value").asText(), "QAO-NC");
        assertEquals(node.at("/collection/operands/1/operands/1/operands/1/value").asText(), "QAO.*");
        assertEquals(node.at("/collection/operation").asText(), "operation:or");
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(ALL)");
        assertEquals(node.at("/collection/rewrites/0/operation").asText(), "operation:insertion");
    }

    @Test
    @DisplayName("Test Search With Corpus Query")
    void testSearchWithCorpusQuery() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("cq", "corpusTitle=gingko").request().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operation").asText(), "operation:and");
        assertEquals(2, node.at("/collection/operands").size());
        assertEquals(node.at("/collection/operands/0/value").asText(), "CC-BY.*");
        assertEquals(node.at("/collection/operands/1/value").asText(), "gingko");
        assertEquals(node.at("/collection/operands/1/match").asText(), "match:eq");
        assertTrue(node.at("/collection/operands/1/type").isMissingNode());
    }

    @Test
    @DisplayName("Test Search Query With Collection Query Authorized Without IP")
    void testSearchQueryWithCollectionQueryAuthorizedWithoutIP() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=das]").queryParam("ql", "poliqarp").queryParam("cq", "textClass=politik & corpusSigle=BRZ10").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015")).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertNotNull(node);
        assertEquals(node.at("/collection/rewrites/0/operation").asText(), "operation:insertion");
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(FREE)");
        // EM: double AND operations
        assertEquals(node.at("/collection/operands/0/key").asText(), "availability");
        assertEquals(node.at("/collection/operands/0/value").asText(), "CC-BY.*");
        assertEquals(node.at("/collection/operands/1/operands/0/key").asText(), "textClass");
        assertEquals(node.at("/collection/operands/1/operands/1/key").asText(), "corpusSigle");
    }

    @Test
    @Disabled
    @DisplayName("Test Search Query Authorized Without IP")
    void testSearchQueryAuthorizedWithoutIP() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=die]").queryParam("ql", "poliqarp").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015")).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals(node.at("/collection/@type").asText(), "koral:docGroup");
        assertEquals(node.at("/collection/operands/0/value").asText(), "CC-BY.*");
        assertEquals(node.at("/collection/operands/1/value").asText(), "ACA.*");
        assertEquals(node.at("/collection/operation").asText(), "operation:or");
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(PUB)");
    }

    @Test
    @DisplayName("Test Search With Invalid Page")
    void testSearchWithInvalidPage() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=die]").queryParam("ql", "poliqarp").queryParam("page", "0").request().get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(), "page must start from 1");
    }

    @Test
    @DisplayName("Test Search Sentence Meta")
    void testSearchSentenceMeta() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("context", "sentence").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(node.at("/meta/context").asText(), "base/s:s");
        assertNotEquals("/meta/version", "${project.version}");
    }

    // EM: The API is disabled
    @Disabled
    @Test
    @DisplayName("Test Search Simple CQL")
    void testSearchSimpleCQL() throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("(der) or (das)", "CQL");
        Response response = target().path(API_VERSION).path("search").request().post(Entity.json(s.toJSON()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        // assertEquals(17027, node.at("/meta/totalResults").asInt());
    }

    // EM: The API is disabled
    @Test
    @Disabled
    @DisplayName("Test Search Raw Query")
    void testSearchRawQuery() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").request().post(Entity.json(createJsonQuery()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(FREE)");
    }

    // EM: The API is disabled
    @Test
    @Disabled
    @DisplayName("Test Search Post All")
    void testSearchPostAll() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").request().header(HttpHeaders.X_FORWARDED_FOR, "10.27.0.32").header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015")).post(Entity.json(createJsonQuery()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(ALL)");
    }

    // EM: The API is disabled
    @Test
    @Disabled
    @DisplayName("Test Search Post Public")
    void testSearchPostPublic() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").request().header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015")).post(Entity.json(createJsonQuery()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(PUB)");
    }
}
