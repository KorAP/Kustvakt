package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class AvailabilityTest extends SpringJerseyTest {

    private void checkAndFree (String json) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(json);
        assertEquals("availability",
                node.at("/collection/operands/0/key").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
        assertEquals("availability(FREE)",
                node.at("/collection/rewrites/0/scope").asText());
    }


    private void checkAndPublic (String json) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(json);
        assertNotNull(node);

        assertEquals("operation:and",
                node.at("/collection/operation").asText());
        assertEquals("match:eq",
                node.at("/collection/operands/0/operands/0/match").asText());
        assertEquals("type:regex",
                node.at("/collection/operands/0/operands/0/type").asText());
        assertEquals("availability",
                node.at("/collection/operands/0/operands/0/key").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/operands/0/value").asText());
        assertEquals("match:eq",
                node.at("/collection/operands/0/operands/1/operands/0/match")
                        .asText());
        assertEquals("ACA.*",
                node.at("/collection/operands/0/operands/1/operands/0/value")
                        .asText());
        assertEquals("match:eq",
                node.at("/collection/operands/0/operands/1/operands/1/match")
                        .asText());
        assertEquals("QAO-NC",
                node.at("/collection/operands/0/operands/1/operands/1/value")
                        .asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
        assertEquals("availability(PUB)",
                node.at("/collection/rewrites/0/scope").asText());
    }

    private void checkAndPublicWithACA (String json) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(json);
        assertNotNull(node);
        assertEquals("operation:and",
                node.at("/collection/operation").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
        assertEquals("availability(PUB)",
                node.at("/collection/rewrites/0/scope").asText());

        assertEquals("match:eq",
                node.at("/collection/operands/1/match").asText());
        assertEquals("type:regex",
                node.at("/collection/operands/1/type").asText());
        assertEquals("availability",
                node.at("/collection/operands/1/key").asText());
        assertEquals("ACA.*", node.at("/collection/operands/1/value").asText());

        node = node.at("/collection/operands/0");
        assertEquals("match:eq", node.at("/operands/0/match").asText());
        assertEquals("type:regex", node.at("/operands/0/type").asText());
        assertEquals("availability", node.at("/operands/0/key").asText());
        assertEquals("CC-BY.*", node.at("/operands/0/value").asText());

        assertEquals("match:eq",
                node.at("/operands/1/operands/0/match").asText());
        assertEquals("type:regex",
                node.at("/operands/1/operands/0/type").asText());
        assertEquals("availability",
                node.at("/operands/1/operands/0/key").asText());
        assertEquals("ACA.*", node.at("/operands/1/operands/0/value").asText());


    }

    private void checkAndAllWithACA (String json) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(json);
        assertNotNull(node);
        assertEquals("operation:and",
                node.at("/collection/operation").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
        assertEquals("availability(ALL)",
                node.at("/collection/rewrites/0/scope").asText());

        assertEquals("match:eq",
                node.at("/collection/operands/1/match").asText());
        assertEquals("type:regex",
                node.at("/collection/operands/1/type").asText());
        assertEquals("availability",
                node.at("/collection/operands/1/key").asText());
        assertEquals("ACA.*", node.at("/collection/operands/1/value").asText());

        node = node.at("/collection/operands/0");

        assertEquals("match:eq", node.at("/operands/0/match").asText());
        assertEquals("type:regex", node.at("/operands/0/type").asText());
        assertEquals("availability", node.at("/operands/0/key").asText());
        assertEquals("CC-BY.*", node.at("/operands/0/value").asText());
        assertEquals("match:eq",
                node.at("/operands/1/operands/1/operands/0/match").asText());
        assertEquals("QAO-NC",
                node.at("/operands/1/operands/1/operands/0/value").asText());
        assertEquals("match:eq",
                node.at("/operands/1/operands/1/operands/1/match").asText());
        assertEquals("QAO.*",
                node.at("/operands/1/operands/1/operands/1/value").asText());

    }



    private ClientResponse searchQuery (String collectionQuery) {
        return resource().path(API_VERSION).path("search").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp").queryParam("cq", collectionQuery)
                .get(ClientResponse.class);
    }


    private ClientResponse searchQueryWithIP (String collectionQuery, String ip)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        return resource().path(API_VERSION).path("search").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp").queryParam("cq", collectionQuery)
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, ip)
                .get(ClientResponse.class);
    }


    @Test
    public void testAvailabilityFreeAuthorized () throws KustvaktException {
        ClientResponse response = searchQuery("availability = CC-BY-SA");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityRegexFreeAuthorized ()
            throws KustvaktException {
        ClientResponse response = searchQuery("availability = /.*BY.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityFreeUnauthorized () throws KustvaktException {
        ClientResponse response = searchQuery("availability = ACA-NC");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityRegexFreeUnauthorized ()
            throws KustvaktException {
        ClientResponse response = searchQuery("availability = /ACA.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }

    @Test
    public void testAvailabilityRegexNoRewrite () throws KustvaktException {
        ClientResponse response = searchQuery(
                "availability = /CC-BY.*/ & availability = /ACA.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String json = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(json);
        assertEquals("operation:and",
                node.at("/collection/operation").asText());
        assertEquals("match:eq",
                node.at("/collection/operands/0/match").asText());
        assertEquals("type:regex",
                node.at("/collection/operands/0/type").asText());
        assertEquals("availability",
                node.at("/collection/operands/0/key").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("match:eq",
                node.at("/collection/operands/1/match").asText());
        assertEquals("ACA.*", node.at("/collection/operands/1/value").asText());

    }


    @Test
    public void testAvailabilityRegexFreeUnauthorized3 ()
            throws KustvaktException {
        ClientResponse response = searchQuery("availability = /.*NC.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        // System.out.println(response.getEntity(String.class));
        checkAndFree(response.getEntity(String.class));
    }



    @Test
    public void testNegationAvailabilityFreeUnauthorized ()
            throws KustvaktException {
        ClientResponse response = searchQuery("availability != /CC-BY.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }

    @Test
    public void testNegationAvailabilityFreeUnauthorized2 ()
            throws KustvaktException {
        ClientResponse response = searchQuery("availability != /.*BY.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }

    @Test
    public void testNegationAvailabilityWithOperationOrUnauthorized ()
            throws KustvaktException {
        ClientResponse response = searchQuery(
                "availability = /CC-BY.*/ | availability != /CC-BY.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        checkAndFree(response.getEntity(String.class));
    }

    @Test
    public void testComplexNegationAvailabilityFreeUnauthorized ()
            throws KustvaktException {
        ClientResponse response =
                searchQuery("textClass=politik & availability != /CC-BY.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testComplexAvailabilityFreeUnauthorized ()
            throws KustvaktException {
        ClientResponse response =
                searchQuery("textClass=politik & availability=ACA-NC");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testComplexAvailabilityFreeUnauthorized3 ()
            throws KustvaktException {
        ClientResponse response =
                searchQuery("textClass=politik & availability=/.*NC.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityPublicAuthorized () throws KustvaktException {
        ClientResponse response =
                searchQueryWithIP("availability=ACA-NC", "149.27.0.32");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublic(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityPublicUnauthorized () throws KustvaktException {
        ClientResponse response =
                searchQueryWithIP("availability=QAO-NC-LOC:ids", "149.27.0.32");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublic(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityRegexPublicAuthorized ()
            throws KustvaktException {
        ClientResponse response =
                searchQueryWithIP("availability= /ACA.*/", "149.27.0.32");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublicWithACA(response.getEntity(String.class));
    }


    @Test
    public void testNegationAvailabilityPublicUnauthorized ()
            throws KustvaktException {
        ClientResponse response =
                searchQueryWithIP("availability != ACA-NC", "149.27.0.32");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublic(response.getEntity(String.class));
    }


    @Test
    public void testNegationAvailabilityRegexPublicUnauthorized ()
            throws KustvaktException {
        ClientResponse response =
                searchQueryWithIP("availability != /ACA.*/", "149.27.0.32");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublic(response.getEntity(String.class));
    }


    @Test
    public void testComplexAvailabilityPublicUnauthorized ()
            throws KustvaktException {
        ClientResponse response = searchQueryWithIP(
                "textClass=politik & availability=QAO-NC-LOC:ids",
                "149.27.0.32");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublic(response.getEntity(String.class));
    }


    @Test
    public void testNegationComplexAvailabilityPublicUnauthorized ()
            throws KustvaktException {
        ClientResponse response = searchQueryWithIP(
                "textClass=politik & availability!=QAO-NC-LOC:ids",
                "149.27.0.32");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublic(response.getEntity(String.class));
    }

    @Test
    public void testAvailabilityRegexAllAuthorized () throws KustvaktException {
        ClientResponse response =
                searchQueryWithIP("availability= /ACA.*/", "10.27.0.32");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndAllWithACA(response.getEntity(String.class));
    }

    @Test
    public void testAvailabilityOr () throws KustvaktException {
        ClientResponse response =
                searchQuery("availability=/CC-BY.*/ | availability=/ACA.*/");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }

    @Test
    public void testRedundancyOrPub () throws KustvaktException {
        ClientResponse response = searchQueryWithIP(
                "availability=/CC-BY.*/ | availability=/ACA.*/ | availability=/QAO-NC/",
                "149.27.0.32");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String json = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(json);
        assertTrue(node.at("/collection/rewrites").isMissingNode());
        assertEquals("operation:or", node.at("/collection/operation").asText());
    }

    @Test
    public void testAvailabilityOrCorpusSigle () throws KustvaktException {
        ClientResponse response =
                searchQuery("availability=/CC-BY.*/ | corpusSigle=GOE");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }

    @Test
    public void testOrWithoutAvailability () throws KustvaktException {
        ClientResponse response =
                searchQuery("corpusSigle=GOE | textClass=politik");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }

    @Test
    public void testWithoutAvailability () throws KustvaktException {
        ClientResponse response = searchQuery("corpusSigle=GOE");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }
}
