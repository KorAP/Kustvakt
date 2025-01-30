package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class AvailabilityTest extends SpringJerseyTest {
    
    @Autowired
    public FullConfiguration config;    

	private void checkAndFree (String json, JsonNode source)
			throws KustvaktException {
		JsonNode node = JsonUtils.readTree(json).at("/collection");
		assertEquals("availability", node.at("/operands/0/key").asText());
		assertEquals("CC.*", node.at("/operands/0/value").asText());
		assertEquals("operation:override",
				node.at("/rewrites/0/operation").asText());
		assertEquals(source, node.at("/rewrites/0/source"));
		//        assertEquals(node.at("/collection/rewrites/0/scope").asText(),
		//                "availability(FREE)");
	}

	private void checkAndPublic (String json, JsonNode source)
			throws KustvaktException {
		JsonNode node = JsonUtils.readTree(json).at("/collection");
		assertNotNull(node);
		assertEquals("operation:and", node.at("/operation").asText());
		assertEquals("koral:rewrite", node.at("/rewrites/0/@type").asText());
		assertEquals("Kustvakt", node.at("/rewrites/0/editor").asText());
		assertEquals("operation:override", node.at("/rewrites/0/operation").asText());
		assertEquals(source, node.at("/rewrites/0/source"));
		
		node = node.at("/operands/0");
		assertEquals("match:eq", node.at("/operands/0/match").asText());
		assertEquals("type:regex", node.at("/operands/0/type").asText());
		assertEquals("availability", node.at("/operands/0/key").asText());
		assertEquals("CC.*", node.at("/operands/0/value").asText());
		assertEquals("match:eq",
				node.at("/operands/1/operands/0/match").asText());
		assertEquals("ACA.*", node.at("/operands/1/operands/0/value").asText());
		assertEquals("match:eq",
				node.at("/operands/1/operands/1/match").asText());
		assertEquals("QAO-NC",
				node.at("/operands/1/operands/1/value").asText());
		
	}

    private void checkAndAllWithACA (String json, JsonNode source)
			throws KustvaktException {
		JsonNode node = JsonUtils.readTree(json).at("/collection");
		assertEquals("operation:and", node.at("/operation").asText());
		assertEquals("operation:and", node.at("/operation").asText());
		assertEquals("koral:rewrite", node.at("/rewrites/0/@type").asText());
		assertEquals("Kustvakt", node.at("/rewrites/0/editor").asText());
		assertEquals("operation:override", node.at("/rewrites/0/operation").asText());
		assertEquals(source, node.at("/rewrites/0/source"));

		assertEquals("match:eq", node.at("/operands/1/match").asText());
		assertEquals("type:regex", node.at("/operands/1/type").asText());
		assertEquals("availability", node.at("/operands/1/key").asText());
        assertEquals("ACA.*", node.at("/operands/1/value").asText());
        node = node.at("/operands/0");
        assertEquals("match:eq", node.at("/operands/0/match").asText());
        assertEquals("type:regex", node.at("/operands/0/type").asText());
        assertEquals("availability", node.at("/operands/0/key").asText());
        assertEquals(config.getFreeOnlyRegex(),
                node.at("/operands/0/value").asText());
        assertEquals("match:eq",
                node.at("/operands/1/operands/1/operands/0/match").asText());
        assertEquals("QAO-NC",
                node.at("/operands/1/operands/1/operands/0/value").asText());
        assertEquals("match:eq",
                node.at("/operands/1/operands/1/operands/1/match").asText());
        assertEquals(config.getAllOnlyRegex(),
                node.at("/operands/1/operands/1/operands/1/value").asText());
    }

    private Response searchQuery (String collectionQuery) {
        return target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("cq", collectionQuery).request().get();
    }

    private Response searchQueryWithIP (String collectionQuery, String ip)
            throws ProcessingException, KustvaktException {
        return target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("cq", collectionQuery).request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
						.createBasicAuthorizationHeaderValue("user", "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, ip).get();
    }

    
    @Test
    public void testFreeWithoutCorpusQuery () throws KustvaktException {
		Response response = target().path(API_VERSION).path("search")
				.queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
				.request().get();
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String json = response.readEntity(String.class);
		JsonNode node = JsonUtils.readTree(json).at("/collection");
		assertEquals("availability", node.at("/key").asText());
		assertEquals("CC.*", node.at("/value").asText());
		assertEquals("operation:injection",
				node.at("/rewrites/0/operation").asText());
    }
    
    @Test
	public void testPublicWithoutCorpusQuery () throws KustvaktException {
		Response response = target().path(API_VERSION).path("search")
				.queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
				.request()
				.header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
						.createBasicAuthorizationHeaderValue("user", "pass"))
				.header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String json = response.readEntity(String.class);
		JsonNode node = JsonUtils.readTree(json).at("/collection");
		
		String expected = """
			{
              "operands" : [ {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "type" : "type:regex",
                "value" : "CC.*",
                "key" : "availability"
              }, {
                "operands" : [ {
                  "@type" : "koral:doc",
                  "match" : "match:eq",
                  "type" : "type:regex",
                  "value" : "ACA.*",
                  "key" : "availability"
                }, {
                  "@type" : "koral:doc",
                  "match" : "match:eq",
                  "type" : "type:regex",
                  "value" : "QAO-NC",
                  "key" : "availability"
                } ],
                "@type" : "koral:docGroup",
                "operation" : "operation:or"
              } ],
              "@type" : "koral:docGroup",
              "operation" : "operation:or",
              "rewrites" : [ {
                "@type" : "koral:rewrite",
                "src" : "Kustvakt",
                "editor" : "Kustvakt",
                "operation" : "operation:injection",
                "scope" : "availability(PUB)"
              } ]
            }
			""";
		
		assertEquals(JsonUtils.readTree(expected), node);
    }
    
    
    @Test
   	public void testAllWithoutCorpusQuery () throws KustvaktException {
   		Response response = target().path(API_VERSION).path("search")
   				.queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
   				.request()
   				.header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
   						.createBasicAuthorizationHeaderValue("user", "pass"))
   				.header(HttpHeaders.X_FORWARDED_FOR, "10.0.10.132").get();
   		assertEquals(Status.OK.getStatusCode(), response.getStatus());
   		
   		String json = response.readEntity(String.class);
   		JsonNode node = JsonUtils.readTree(json).at("/collection");
   		String expected = """
   			{
              "operands" : [ {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "type" : "type:regex",
                "value" : "CC.*",
                "key" : "availability"
              }, {
                "operands" : [ {
                  "@type" : "koral:doc",
                  "match" : "match:eq",
                  "type" : "type:regex",
                  "value" : "ACA.*",
                  "key" : "availability"
                }, {
                  "operands" : [ {
                    "@type" : "koral:doc",
                    "match" : "match:eq",
                    "type" : "type:regex",
                    "value" : "QAO-NC",
                    "key" : "availability"
                  }, {
                    "@type" : "koral:doc",
                    "match" : "match:eq",
                    "type" : "type:regex",
                    "value" : "QAO-NC-LOC:ids.*",
                    "key" : "availability"
                  } ],
                  "@type" : "koral:docGroup",
                  "operation" : "operation:or"
                } ],
                "@type" : "koral:docGroup",
                "operation" : "operation:or"
              } ],
              "@type" : "koral:docGroup",
              "operation" : "operation:or",
              "rewrites" : [ {
                "@type" : "koral:rewrite",
                "src" : "Kustvakt",
                "editor" : "Kustvakt",
                "operation" : "operation:injection",
                "scope" : "availability(ALL)"
              } ]
            }
   			""";
   		assertEquals(JsonUtils.readTree(expected), node);
    }
    
    @Test
    public void testFreeWithoutAvailabilityOr () throws KustvaktException {
        Response response = searchQuery("corpusSigle=GOE | textClass=politik");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
            {
              "operands" : [ {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "GOE",
                "key" : "corpusSigle"
              }, {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "politik",
                "key" : "textClass"
              } ],
              "@type" : "koral:docGroup",
              "operation" : "operation:or"
            }	
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testFreeWithoutAvailability () throws KustvaktException {
        Response response = searchQuery("corpusSigle=GOE");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
            {
              "@type" : "koral:doc",
              "match" : "match:eq",
              "value" : "GOE",
              "key" : "corpusSigle"
            }	
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    
    @Test
    public void testFreeAvailabilityNoRewrite () throws KustvaktException {
        Response response = searchQuery("availability = /CC.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String json = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(json);
        assertTrue(node.at("/collection/rewrite").isMissingNode());
    }
    
    @Test
    public void testFreeAvailabilityNoRewriteAnd () throws KustvaktException {
        Response response = searchQuery(
                "availability = /CC.*/ & availability = /ACA.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String json = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(json);
        assertEquals(node.at("/collection/operation").asText(),
                "operation:and");
        assertEquals(node.at("/collection/operands/0/match").asText(),
                "match:eq");
        assertEquals(node.at("/collection/operands/0/type").asText(),
                "type:regex");
        assertEquals(node.at("/collection/operands/0/key").asText(),
                "availability");
        assertEquals(node.at("/collection/operands/0/value").asText(),
                "CC.*");
        assertEquals(node.at("/collection/operands/1/match").asText(),
                "match:eq");
        assertEquals(node.at("/collection/operands/1/value").asText(), "ACA.*");
    }

    
    @Test
    public void testFreeAvailabilityAuthorized () throws KustvaktException {
        Response response = searchQuery("availability = CC-BY-SA");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        String source = """
        	{
              "@type" : "koral:doc",
              "match" : "match:eq",
              "value" : "CC-BY-SA",
              "key" : "availability"
            }
        	""";
        
		checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }
    
    @Test
    public void testFreeAvailabilityUnauthorized () throws KustvaktException {
        Response response = searchQuery("availability = ACA-NC");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
              "@type" : "koral:doc",
              "match" : "match:eq",
              "value" : "ACA-NC",
              "key" : "availability"
            }
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testFreeAvailabilityRegexAuthorized ()
            throws KustvaktException {
        Response response = searchQuery("availability = /.*BY.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
              "@type" : "koral:doc",
              "match" : "match:eq",
              "type" : "type:regex",
              "value" : ".*BY.*",
              "key" : "availability"
            }
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    
    @Test
    public void testFreeAvailabilityRegexUnauthorized ()
            throws KustvaktException {
        Response response = searchQuery("availability = /ACA.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
              "@type" : "koral:doc",
              "match" : "match:eq",
              "type" : "type:regex",
              "value" : "ACA.*",
              "key" : "availability"
            }
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }
    
    @Test
    public void testFreeAvailabilityRegexUnauthorized2 ()
            throws KustvaktException {
        Response response = searchQuery("availability = /.*NC.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
              "@type" : "koral:doc",
              "match" : "match:eq",
              "type" : "type:regex",
              "value" : ".*NC.*",
              "key" : "availability"
            }
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }
    
    @Test
    public void testFreeAvailabilityOr () throws KustvaktException {
        Response response = searchQuery(
                "availability=/CC.*/ | availability=/ACA.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
              "operands" : [ {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "type" : "type:regex",
                "value" : "CC.*",
                "key" : "availability"
              }, {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "type" : "type:regex",
                "value" : "ACA.*",
                "key" : "availability"
              } ],
              "@type" : "koral:docGroup",
              "operation" : "operation:or"
            }
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }
    @Test
    public void testFreeAvailabilityOrCorpusSigle () throws KustvaktException {
        Response response = searchQuery(
                "availability=/CC.*/ | corpusSigle=GOE");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
            {
              "operands" : [ {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "type" : "type:regex",
                "value" : "CC.*",
                "key" : "availability"
              }, {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "GOE",
                "key" : "corpusSigle"
              } ],
              "@type" : "koral:docGroup",
              "operation" : "operation:or"
            }	
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    
    @Test
    public void testFreeAvailabilityNegationUnauthorized ()
            throws KustvaktException {
        Response response = searchQuery("availability != /CC.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
              "@type" : "koral:doc",
              "match" : "match:ne",
              "type" : "type:regex",
              "value" : "CC.*",
              "key" : "availability"
            }
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testFreeAvailabilityNegationUnauthorized2 ()
            throws KustvaktException {
        Response response = searchQuery("availability != /.*BY.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
              "@type" : "koral:doc",
              "match" : "match:ne",
              "type" : "type:regex",
              "value" : ".*BY.*",
              "key" : "availability"
            }
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testFreeAvailabilityNegationOrUnauthorized ()
            throws KustvaktException {
        Response response = searchQuery(
                "availability = /CC.*/ | availability != /CC.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
              "operands" : [ {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "type" : "type:regex",
                "value" : "CC.*",
                "key" : "availability"
              }, {
                "@type" : "koral:doc",
                "match" : "match:ne",
                "type" : "type:regex",
                "value" : "CC.*",
                "key" : "availability"
              } ],
              "@type" : "koral:docGroup",
              "operation" : "operation:or"
            }
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testFreeAvailabilityNegationAndUnauthorized ()
            throws KustvaktException {
        Response response = searchQuery(
                "textClass=politik & availability != /CC.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
              "operands" : [ {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "politik",
                "key" : "textClass"
              }, {
                "@type" : "koral:doc",
                "match" : "match:ne",
                "type" : "type:regex",
                "value" : "CC.*",
                "key" : "availability"
              } ],
              "@type" : "koral:docGroup",
              "operation" : "operation:and"
            }
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testFreeAvailabilityAndUnauthorized ()
            throws KustvaktException {
        Response response = searchQuery(
                "textClass=politik & availability=ACA-NC");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
              "operands" : [ {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "politik",
                "key" : "textClass"
              }, {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "ACA-NC",
                "key" : "availability"
              } ],
              "@type" : "koral:docGroup",
              "operation" : "operation:and"
            }
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testFreeAvailabilityAndUnauthorized2 ()
            throws KustvaktException {
        Response response = searchQuery(
                "textClass=politik & availability=/.*NC.*/");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
              "operands" : [ {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "politik",
                "key" : "textClass"
              }, {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "type" : "type:regex",
                "value" : ".*NC.*",
                "key" : "availability"
              } ],
              "@type" : "koral:docGroup",
              "operation" : "operation:and"
            }
        	""";
        checkAndFree(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }
    
    @Test
    public void testPublicAvailabilityNoRewrite () throws KustvaktException {
        Response response = searchQueryWithIP(
                "availability=/CC.*/ | availability=/ACA.*/ | availability=/QAO-NC/",
                "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String json = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(json);
        assertTrue(node.at("/collection/rewrites").isMissingNode());
        assertEquals(node.at("/collection/operation").asText(), "operation:or");
    }

    @Test
    public void testPublicAvailabilityAuthorized () throws KustvaktException {
        Response response = searchQueryWithIP("availability=ACA-NC",
                "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "ACA-NC",
                "key" : "availability"
            }
        	""";
        
		checkAndPublic(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testPublicAvailabilityUnauthorized () throws KustvaktException {
        Response response = searchQueryWithIP("availability=QAO-NC-LOC:ids",
                "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        String source = """
        	{
                  "@type" : "koral:doc",
                  "match" : "match:eq",
                  "value" : "QAO-NC-LOC:ids",
                  "key" : "availability"
            }
        	""";
		checkAndPublic(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testPublicAvailabilityRegexAuthorized ()
            throws KustvaktException {
        Response response = searchQueryWithIP("availability= /ACA.*/",
                "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
                "@type" : "koral:doc",
                "match" : "match:eq",
                "type" : "type:regex",
                "value" : "ACA.*",
                "key" : "availability"
            }
            	""";
		checkAndPublic(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testPublicAvailabilityNegationUnauthorized ()
            throws KustvaktException {
        Response response = searchQueryWithIP("availability != ACA-NC",
                "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
                {
                  "@type" : "koral:doc",
                  "match" : "match:ne",
                  "value" : "ACA-NC",
                  "key" : "availability"
                }	
        	""";
		checkAndPublic(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testPublicAvailabilityNegationRegexUnauthorized ()
            throws KustvaktException {
        Response response = searchQueryWithIP("availability != /ACA.*/",
                "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
            {
              "@type" : "koral:doc",
              "match" : "match:ne",
              "type" : "type:regex",
              "value" : "ACA.*",
              "key" : "availability"
            }	
        	""";
		checkAndPublic(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testPublicAvailabilityAndUnauthorized ()
            throws KustvaktException {
        Response response = searchQueryWithIP(
                "textClass=politik & availability=QAO-NC-LOC:ids",
                "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
            {
              "operands" : [ {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "politik",
                "key" : "textClass"
              }, {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "QAO-NC-LOC:ids",
                "key" : "availability"
              } ],
              "@type" : "koral:docGroup",
              "operation" : "operation:and"
            }	
        	""";
		checkAndPublic(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testPublicAvailabilityNegationAndUnauthorized ()
            throws KustvaktException {
        Response response = searchQueryWithIP(
                "textClass=politik & availability!=QAO-NC-LOC:ids",
                "149.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
            {
              "operands" : [ {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "politik",
                "key" : "textClass"
              }, {
                "@type" : "koral:doc",
                "match" : "match:ne",
                "value" : "QAO-NC-LOC:ids",
                "key" : "availability"
              } ],
              "@type" : "koral:docGroup",
              "operation" : "operation:and"
            }	
        	""";
		checkAndPublic(response.readEntity(String.class),
				JsonUtils.readTree(source));
    }

    @Test
    public void testAllAvailabilityRegexAuthorized () throws KustvaktException {
        Response response = searchQueryWithIP("availability= /ACA.*/",
                "10.27.0.32");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String source = """
        	{
                "@type" : "koral:doc",
                "match" : "match:eq",
                "type" : "type:regex",
                "value" : "ACA.*",
                "key" : "availability"
              }
            }
        	""";
        checkAndAllWithACA(response.readEntity(String.class),
        		JsonUtils.readTree(source));
    }
}
