package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.jetty.http.HttpHeaders;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

public class SearchWithAvailabilityTest extends FastJerseyTest {

    @Override
    public void initMethod () throws KustvaktException {
//        helper().runBootInterfaces();
    }


    @BeforeClass
    public static void configure () throws Exception {
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }

    private void checkAndFree (String json) {
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


    private void checkAndPublic (String json) {
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
                node.at("/collection/operands/0/operands/1/match").asText());
        assertEquals("ACA.*",
                node.at("/collection/operands/0/operands/1/value").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
        assertEquals("availability(PUB)",
                node.at("/collection/rewrites/0/scope").asText());
    }
    
    private void checkAndPublicWithoutACA (String json) {
        JsonNode node = JsonUtils.readTree(json);
        assertNotNull(node);
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
        assertEquals("type:regex",
                node.at("/collection/operands/1/type").asText());
        assertEquals("availability",
                node.at("/collection/operands/1/key").asText());
        assertEquals("ACA.*",
                node.at("/collection/operands/1/value").asText());
        
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
        assertEquals("availability(PUB)",
                node.at("/collection/rewrites/0/scope").asText());
    }
    
    private void checkAndAll (String json) {
        JsonNode node = JsonUtils.readTree(json);
        assertNotNull(node);
        assertEquals("availability(ALL)",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
        
        assertEquals("operation:and",
                node.at("/collection/operation").asText());
        
        node = node.at("/collection/operands/0");
        assertEquals("operation:or",
                node.at("/operation").asText());
        
        assertEquals("match:eq",
                node.at("/operands/0/match").asText());
        assertEquals("match:eq",
                node.at("/operands/0/match").asText());
        assertEquals("type:regex",
                node.at("/operands/0/type").asText());
        assertEquals("availability",
                node.at("/operands/0/key").asText());
        assertEquals("CC-BY.*",
                node.at("/operands/0/value").asText());
        
        node = node.at("/operands/1");
        assertEquals("operation:or",
                node.at("/operation").asText());
        assertEquals("match:eq",
                node.at("/operands/0/match").asText());
        assertEquals("ACA.*",
                node.at("/operands/0/value").asText());
        assertEquals("match:eq",
                node.at("/operands/1/match").asText());
        assertEquals("QAO.*",
                node.at("/operands/1/value").asText());
        
    }

    private void checkAndAllWithoutACA (String json) {
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
                node.at("/collection/operands/0/operands/1/match").asText());
        assertEquals("QAO.*",
                node.at("/collection/operands/0/operands/1/value").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
        assertEquals("availability(ALL)",
                node.at("/collection/rewrites/0/scope").asText());
    }
    
    

    private ClientResponse builtSimpleClientResponse (String collectionQuery) {
        return resource().path(getAPIVersion()).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("cq", collectionQuery).get(ClientResponse.class);
    }


    private ClientResponse builtClientResponseWithIP (String collectionQuery,
            String ip) {
        return resource().path(getAPIVersion()).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("cq", collectionQuery)
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, ip)
                .get(ClientResponse.class);
    }


    @Test
    public void testAvailabilityFreeAuthorized () {
        ClientResponse response = builtSimpleClientResponse(
                "availability = CC-BY-SA");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityRegexFreeAuthorized () {
        ClientResponse response = builtSimpleClientResponse(
                "availability = /.*BY.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityFreeUnauthorized () {
        ClientResponse response = builtSimpleClientResponse(
                "availability = ACA-NC");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityRegexFreeUnauthorized () {
        ClientResponse response = builtSimpleClientResponse(
                "availability = /ACA.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }

    @Test
    public void testAvailabilityRegexNoRewrite () {
        ClientResponse response = builtSimpleClientResponse(
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
    public void testAvailabilityRegexFreeUnauthorized3 () {
        ClientResponse response = builtSimpleClientResponse(
                "availability = /.*NC.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }



    @Test
    public void testNegationAvailabilityFreeUnauthorized () {
        ClientResponse response = builtSimpleClientResponse(
                "availability != /CC-BY.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testNegationAvailabilityFreeUnauthorized2 () {
        ClientResponse response = builtSimpleClientResponse(
                "availability != /.*BY.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testComplexNegationAvailabilityFreeUnauthorized () {
        ClientResponse response = builtSimpleClientResponse(
                "textClass=politik & availability != /CC-BY.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testComplexAvailabilityFreeUnauthorized () {
        ClientResponse response = builtSimpleClientResponse(
                "textClass=politik & availability=ACA-NC");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testComplexAvailabilityFreeUnauthorized3 () {
        ClientResponse response = builtSimpleClientResponse(
                "textClass=politik & availability=/.*NC.*/");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndFree(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityPublicAuthorized () {
        ClientResponse response = builtClientResponseWithIP(
                "availability=ACA-NC", "149.27.0.32");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        
        checkAndPublic(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityPublicUnauthorized () {
        ClientResponse response = builtClientResponseWithIP(
                "availability=QAO-NC-LOC:ids", "149.27.0.32");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublic(response.getEntity(String.class));
    }


    @Test
    public void testAvailabilityRegexPublicAuthorized () {
        ClientResponse response = builtClientResponseWithIP(
                "availability= /ACA.*/", "149.27.0.32");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublicWithoutACA(response.getEntity(String.class));
    }


    @Test
    public void testNegationAvailabilityPublicUnauthorized () {
        ClientResponse response = builtClientResponseWithIP(
                "availability != ACA-NC", "149.27.0.32");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublic(response.getEntity(String.class));
    }


    @Test
    public void testNegationAvailabilityRegexPublicUnauthorized () {
        ClientResponse response = builtClientResponseWithIP(
                "availability != /ACA.*/", "149.27.0.32");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublic(response.getEntity(String.class));
    }


    @Test
    public void testComplexAvailabilityPublicUnauthorized () {
        ClientResponse response = builtClientResponseWithIP(
                "textClass=politik & availability=QAO-NC-LOC:ids",
                "149.27.0.32");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublic(response.getEntity(String.class));
    }


    @Test
    public void testNegationComplexAvailabilityPublicUnauthorized () {
        ClientResponse response = builtClientResponseWithIP(
                "textClass=politik & availability!=QAO-NC-LOC:ids",
                "149.27.0.32");

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndPublic(response.getEntity(String.class));
    }

    @Test
    public void testAvailabilityRegexAllAuthorized () {
        ClientResponse response = builtClientResponseWithIP(
                "availability= /ACA.*/", "10.27.0.32");
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        checkAndAllWithoutACA(response.getEntity(String.class));
    }
    
}
