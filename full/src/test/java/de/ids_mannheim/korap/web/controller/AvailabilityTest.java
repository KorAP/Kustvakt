package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Availability Test")
class AvailabilityTest extends SpringJerseyTest {

    private void checkAndFree(String json) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(json);
        assertEquals(node.at("/collection/operands/0/key").asText(), "availability");
        assertEquals(node.at("/collection/operands/0/value").asText(), "CC-BY.*");
        assertEquals(node.at("/collection/rewrites/0/operation").asText(), "operation:insertion");
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(FREE)");
    }

    private void checkAndPublic(String json) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(json);
        assertNotNull(node);
        assertEquals(node.at("/collection/operation").asText(), "operation:and");
        assertEquals(node.at("/collection/operands/0/operands/0/match").asText(), "match:eq");
        assertEquals(node.at("/collection/operands/0/operands/0/type").asText(), "type:regex");
        assertEquals(node.at("/collection/operands/0/operands/0/key").asText(), "availability");
        assertEquals(node.at("/collection/operands/0/operands/0/value").asText(), "CC-BY.*");
        assertEquals(node.at("/collection/operands/0/operands/1/operands/0/match").asText(), "match:eq");
        assertEquals(node.at("/collection/operands/0/operands/1/operands/0/value").asText(), "ACA.*");
        assertEquals(node.at("/collection/operands/0/operands/1/operands/1/match").asText(), "match:eq");
        assertEquals(node.at("/collection/operands/0/operands/1/operands/1/value").asText(), "QAO-NC");
        assertEquals(node.at("/collection/rewrites/0/operation").asText(), "operation:insertion");
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(PUB)");
    }

    private void checkAndPublicWithACA(String json) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(json);
        assertNotNull(node);
        assertEquals(node.at("/collection/operation").asText(), "operation:and");
        assertEquals(node.at("/collection/rewrites/0/operation").asText(), "operation:insertion");
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(PUB)");
        assertEquals(node.at("/collection/operands/1/match").asText(), "match:eq");
        assertEquals(node.at("/collection/operands/1/type").asText(), "type:regex");
        assertEquals(node.at("/collection/operands/1/key").asText(), "availability");
        assertEquals(node.at("/collection/operands/1/value").asText(), "ACA.*");
        node = node.at("/collection/operands/0");
        assertEquals(node.at("/operands/0/match").asText(), "match:eq");
        assertEquals(node.at("/operands/0/type").asText(), "type:regex");
        assertEquals(node.at("/operands/0/key").asText(), "availability");
        assertEquals(node.at("/operands/0/value").asText(), "CC-BY.*");
        assertEquals(node.at("/operands/1/operands/0/match").asText(), "match:eq");
        assertEquals(node.at("/operands/1/operands/0/type").asText(), "type:regex");
        assertEquals(node.at("/operands/1/operands/0/key").asText(), "availability");
        assertEquals(node.at("/operands/1/operands/0/value").asText(), "ACA.*");
    }

    private void checkAndAllWithACA(String json) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(json);
        assertNotNull(node);
        assertEquals(node.at("/collection/operation").asText(), "operation:and");
        assertEquals(node.at("/collection/rewrites/0/operation").asText(), "operation:insertion");
        assertEquals(node.at("/collection/rewrites/0/scope").asText(), "availability(ALL)");
        assertEquals(node.at("/collection/operands/1/match").asText(), "match:eq");
        assertEquals(node.at("/collection/operands/1/type").asText(), "type:regex");
        assertEquals(node.at("/collection/operands/1/key").asText(), "availability");
        assertEquals(node.at("/collection/operands/1/value").asText(), "ACA.*");
        node = node.at("/collection/operands/0");
        assertEquals(node.at("/operands/0/match").asText(), "match:eq");
        assertEquals(node.at("/operands/0/type").asText(), "type:regex");
        assertEquals(node.at("/operands/0/key").asText(), "availability");
        assertEquals(node.at("/operands/0/value").asText(), "CC-BY.*");
        assertEquals(node.at("/operands/1/operands/1/operands/0/match").asText(), "match:eq");
        assertEquals(node.at("/operands/1/operands/1/operands/0/value").asText(), "QAO-NC");
        assertEquals(node.at("/operands/1/operands/1/operands/1/match").asText(), "match:eq");
        assertEquals(node.at("/operands/1/operands/1/operands/1/value").asText(), "QAO.*");
    }

    private Response searchQuery(String collectionQuery) {
        return target().path(API_VERSION).path("search").queryParam("q", "[orth=das]").queryParam("ql", "poliqarp").queryParam("cq", collectionQuery).request().get();
    }

    private Response searchQueryWithIP(String collectionQuery, String ip) throws ProcessingException, KustvaktException {
        return target().path(API_VERSION).path("search").queryParam("q", "[orth=das]").queryParam("ql", "poliqarp").queryParam("cq", collectionQuery).request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015")).header(HttpHeaders.X_FORWARDED_FOR, ip).get();
    }

    @Test
    @DisplayName("Test Availability Free Authorized")
    void testAvailabilityFreeAuthorized() throws KustvaktException {
        Response response = searchQuery("availability = CC-BY-SA");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Availability Regex Free Authorized")
    void testAvailabilityRegexFreeAuthorized() throws KustvaktException {
        Response response = searchQuery("availability = /.*BY.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Availability Free Unauthorized")
    void testAvailabilityFreeUnauthorized() throws KustvaktException {
        Response response = searchQuery("availability = ACA-NC");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Availability Regex Free Unauthorized")
    void testAvailabilityRegexFreeUnauthorized() throws KustvaktException {
        Response response = searchQuery("availability = /ACA.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Availability Regex No Rewrite")
    void testAvailabilityRegexNoRewrite() throws KustvaktException {
        Response response = searchQuery("availability = /CC-BY.*/ & availability = /ACA.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String json = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(json);
        assertEquals(node.at("/collection/operation").asText(), "operation:and");
        assertEquals(node.at("/collection/operands/0/match").asText(), "match:eq");
        assertEquals(node.at("/collection/operands/0/type").asText(), "type:regex");
        assertEquals(node.at("/collection/operands/0/key").asText(), "availability");
        assertEquals(node.at("/collection/operands/0/value").asText(), "CC-BY.*");
        assertEquals(node.at("/collection/operands/1/match").asText(), "match:eq");
        assertEquals(node.at("/collection/operands/1/value").asText(), "ACA.*");
    }

    @Test
    @DisplayName("Test Availability Regex Free Unauthorized 3")
    void testAvailabilityRegexFreeUnauthorized3() throws KustvaktException {
        Response response = searchQuery("availability = /.*NC.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        // System.out.println(response.readEntity(String.class));
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Negation Availability Free Unauthorized")
    void testNegationAvailabilityFreeUnauthorized() throws KustvaktException {
        Response response = searchQuery("availability != /CC-BY.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Negation Availability Free Unauthorized 2")
    void testNegationAvailabilityFreeUnauthorized2() throws KustvaktException {
        Response response = searchQuery("availability != /.*BY.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Negation Availability With Operation Or Unauthorized")
    void testNegationAvailabilityWithOperationOrUnauthorized() throws KustvaktException {
        Response response = searchQuery("availability = /CC-BY.*/ | availability != /CC-BY.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Complex Negation Availability Free Unauthorized")
    void testComplexNegationAvailabilityFreeUnauthorized() throws KustvaktException {
        Response response = searchQuery("textClass=politik & availability != /CC-BY.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Complex Availability Free Unauthorized")
    void testComplexAvailabilityFreeUnauthorized() throws KustvaktException {
        Response response = searchQuery("textClass=politik & availability=ACA-NC");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Complex Availability Free Unauthorized 3")
    void testComplexAvailabilityFreeUnauthorized3() throws KustvaktException {
        Response response = searchQuery("textClass=politik & availability=/.*NC.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Availability Public Authorized")
    void testAvailabilityPublicAuthorized() throws KustvaktException {
        Response response = searchQueryWithIP("availability=ACA-NC", "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndPublic(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Availability Public Unauthorized")
    void testAvailabilityPublicUnauthorized() throws KustvaktException {
        Response response = searchQueryWithIP("availability=QAO-NC-LOC:ids", "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndPublic(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Availability Regex Public Authorized")
    void testAvailabilityRegexPublicAuthorized() throws KustvaktException {
        Response response = searchQueryWithIP("availability= /ACA.*/", "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndPublicWithACA(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Negation Availability Public Unauthorized")
    void testNegationAvailabilityPublicUnauthorized() throws KustvaktException {
        Response response = searchQueryWithIP("availability != ACA-NC", "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndPublic(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Negation Availability Regex Public Unauthorized")
    void testNegationAvailabilityRegexPublicUnauthorized() throws KustvaktException {
        Response response = searchQueryWithIP("availability != /ACA.*/", "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndPublic(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Complex Availability Public Unauthorized")
    void testComplexAvailabilityPublicUnauthorized() throws KustvaktException {
        Response response = searchQueryWithIP("textClass=politik & availability=QAO-NC-LOC:ids", "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndPublic(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Negation Complex Availability Public Unauthorized")
    void testNegationComplexAvailabilityPublicUnauthorized() throws KustvaktException {
        Response response = searchQueryWithIP("textClass=politik & availability!=QAO-NC-LOC:ids", "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndPublic(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Availability Regex All Authorized")
    void testAvailabilityRegexAllAuthorized() throws KustvaktException {
        Response response = searchQueryWithIP("availability= /ACA.*/", "10.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndAllWithACA(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Availability Or")
    void testAvailabilityOr() throws KustvaktException {
        Response response = searchQuery("availability=/CC-BY.*/ | availability=/ACA.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Redundancy Or Pub")
    void testRedundancyOrPub() throws KustvaktException {
        Response response = searchQueryWithIP("availability=/CC-BY.*/ | availability=/ACA.*/ | availability=/QAO-NC/", "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String json = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(json);
        assertTrue(node.at("/collection/rewrites").isMissingNode());
        assertEquals(node.at("/collection/operation").asText(), "operation:or");
    }

    @Test
    @DisplayName("Test Availability Or Corpus Sigle")
    void testAvailabilityOrCorpusSigle() throws KustvaktException {
        Response response = searchQuery("availability=/CC-BY.*/ | corpusSigle=GOE");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Or Without Availability")
    void testOrWithoutAvailability() throws KustvaktException {
        Response response = searchQuery("corpusSigle=GOE | textClass=politik");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }

    @Test
    @DisplayName("Test Without Availability")
    void testWithoutAvailability() throws KustvaktException {
        Response response = searchQuery("corpusSigle=GOE");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        checkAndFree(response.readEntity(String.class));
    }
}
