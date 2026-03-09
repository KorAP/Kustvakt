package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.controller.oauth2.OAuth2TestBase;

public class QuerySerializationControllerTest extends OAuth2TestBase {

    @Test
    public void testQuerySerializationWithCorpusQuery ()
            throws KustvaktException {
        Response response = target().path(API_VERSION)
                .path("serialize").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .queryParam("cq", "corpusSigle=WPD13")
                .request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        
        String expectedJson = """
        {
          "meta" : {
            "context" : "base/s:s",
            "tokens" : false,
            "snippets" : true,
            "timeout" : 10000
          },
          "query" : {
            "@type" : "koral:token",
            "wrap" : {
              "@type" : "koral:term",
              "match" : "match:eq",
              "key" : "der",
              "layer" : "orth",
              "foundry" : "opennlp",
              "rewrites" : [ {
                "@type" : "koral:rewrite",
                "src" : "Kustvakt",
                "editor" : "Kustvakt",
                "operation" : "operation:injection",
                "scope" : "foundry",
                "_comment" : "Default foundry has been added."
              } ]
            }
          },
          "corpus" : {
            "@type" : "koral:docGroup",
            "operation" : "operation:and",
            "operands" : [ {
              "@type" : "koral:doc",
              "match" : "match:eq",
              "type" : "type:regex",
              "value" : "CC.*",
              "key" : "availability"
            }, {
              "@type" : "koral:doc",
              "match" : "match:eq",
              "value" : "WPD13",
              "key" : "corpusSigle"
            } ],
            "rewrites" : [ {
              "@type" : "koral:rewrite",
              "src" : "Kustvakt",
              "editor" : "Kustvakt",
              "operation" : "operation:override",
              "original" : {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "WPD13",
                "key" : "corpusSigle"
              },
              "_comment" : "Free corpus access policy has been added."
            } ]
          },
          "@context" : "http://korap.ids-mannheim.de/ns/koral/0.3/context.jsonld"
        }
        """;
        
        JsonNode expectedNode = JsonUtils.readTree(expectedJson);
        
        assertNotNull(node);
        assertEquals(expectedNode, node);
    }


    @Test
    public void testQuerySerializationWithPublicAccess ()
            throws KustvaktException {
        Response tokenResponse = requestTokenWithDoryPassword(superClientId, 
        		clientSecret);
        String tokenResponseEntity = tokenResponse.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), tokenResponse.getStatus());
        
        JsonNode tokenNode = JsonUtils.readTree(tokenResponseEntity);
        String accessToken = tokenNode.at("/access_token").asText();
        assertNotNull(accessToken);
        
        Response response = target().path(API_VERSION)
                .path("serialize").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp")
                .queryParam("cq", "corpusSigle=BRZ10")
                .request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                // EM: without X-Forwarded-For header, the request is only granted 
                // free access, see KustvaktAuthenticationManager
                // .setAccessAndLocation(User, HttpHeaders)
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);

        String expectedJson = """
    	{
          "meta" : {
            "snippets" : true,
            "tokens" : false,
            "timeout" : 90000
          },
          "query" : {
            "@type" : "koral:token",
            "wrap" : {
              "@type" : "koral:term",
              "match" : "match:eq",
              "key" : "der",
              "layer" : "orth",
              "foundry" : "opennlp",
              "rewrites" : [ {
                "@type" : "koral:rewrite",
                "src" : "Kustvakt",
                "editor" : "Kustvakt",
                "operation" : "operation:injection",
                "scope" : "foundry",
                "_comment" : "Default foundry has been added."
              } ]
            }
          },
          "corpus" : {
            "@type" : "koral:docGroup",
            "operation" : "operation:and",
            "operands" : [ {
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
              "operation" : "operation:or"
            }, {
              "@type" : "koral:doc",
              "match" : "match:eq",
              "value" : "BRZ10",
              "key" : "corpusSigle"
            } ],
            "rewrites" : [ {
              "@type" : "koral:rewrite",
              "src" : "Kustvakt",
              "editor" : "Kustvakt",
              "operation" : "operation:override",
              "original" : {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "BRZ10",
                "key" : "corpusSigle"
              },
              "_comment" : "Public corpus access policy has been added."
            } ]
          },
          "@context" : "http://korap.ids-mannheim.de/ns/koral/0.3/context.jsonld"
        }

        	""";
        JsonNode expectedNode = JsonUtils.readTree(expectedJson);
        assertNotNull(node);
        assertEquals(expectedNode, node);
        // Clean up
        revokeToken(accessToken, superClientId, clientSecret, ACCESS_TOKEN_TYPE);
    }

    @Test
    public void testQuerySerializationWithAllAccess ()
            throws KustvaktException {
        Response tokenResponse = requestTokenWithDoryPassword(superClientId, 
        		clientSecret);
        String tokenResponseEntity = tokenResponse.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), tokenResponse.getStatus());
        
        JsonNode tokenNode = JsonUtils.readTree(tokenResponseEntity);
        String accessToken = tokenNode.at("/access_token").asText();
        assertNotNull(accessToken);
        
        Response response = target().path(API_VERSION)
                .path("serialize").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp")
                .queryParam("cq", "corpusSigle=BRZ10")
                .request()
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.X_FORWARDED_FOR, "10.7.0.15")
                .method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);

        String expectedJson = """
    	{
          "meta" : {
            "snippets" : true,
            "tokens" : false,
            "timeout" : 90000
          },
          "query" : {
            "@type" : "koral:token",
            "wrap" : {
              "@type" : "koral:term",
              "match" : "match:eq",
              "key" : "der",
              "layer" : "orth",
              "foundry" : "opennlp",
              "rewrites" : [ {
                "@type" : "koral:rewrite",
                "src" : "Kustvakt",
                "editor" : "Kustvakt",
                "operation" : "operation:injection",
                "scope" : "foundry",
                "_comment" : "Default foundry has been added."
              } ]
            }
          },
          "corpus" : {
            "@type" : "koral:docGroup",
            "operation" : "operation:and",
            "operands" : [ {
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
              "operation" : "operation:or"
            }, {
              "@type" : "koral:doc",
              "match" : "match:eq",
              "value" : "BRZ10",
              "key" : "corpusSigle"
            } ],
            "rewrites" : [ {
              "@type" : "koral:rewrite",
              "src" : "Kustvakt",
              "editor" : "Kustvakt",
              "operation" : "operation:override",
              "original" : {
                "@type" : "koral:doc",
                "match" : "match:eq",
                "value" : "BRZ10",
                "key" : "corpusSigle"
              },
              "_comment" : "All corpus access policy has been added."
            } ]
          },
          "@context" : "http://korap.ids-mannheim.de/ns/koral/0.3/context.jsonld"
        }

        	""";
        JsonNode expectedNode = JsonUtils.readTree(expectedJson);
        assertNotNull(node);
        assertEquals(expectedNode, node);
        // Clean up
        revokeToken(accessToken, superClientId, clientSecret, ACCESS_TOKEN_TYPE);
    }
    
    @Test
    public void testQuerySerializationWithVCRef ()
            throws KustvaktException {
            Response response = target().path(API_VERSION)
                    .path("serialize").queryParam("q", "[orth=der]")
                    .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                    .queryParam("cq", "referTo system-vc")
                    .request().method("GET");
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            String ent = response.readEntity(String.class);
            JsonNode node = JsonUtils.readTree(ent);
            
            String expectedJson = """
        	{
              "meta" : {
                "context" : "base/s:s",
                "tokens" : false,
                "snippets" : true,
                "timeout" : 10000
              },
              "query" : {
                "@type" : "koral:token",
                "wrap" : {
                  "@type" : "koral:term",
                  "match" : "match:eq",
                  "key" : "der",
                  "layer" : "orth",
                  "foundry" : "opennlp",
                  "rewrites" : [ {
                    "@type" : "koral:rewrite",
                    "src" : "Kustvakt",
                    "editor" : "Kustvakt",
                    "operation" : "operation:injection",
                    "scope" : "foundry",
                    "_comment" : "Default foundry has been added."
                  } ]
                }
              },
              "corpus" : {
                "@type" : "koral:docGroup",
                "operation" : "operation:and",
                "operands" : [ {
                  "@type" : "koral:doc",
                  "match" : "match:eq",
                  "type" : "type:regex",
                  "value" : "CC.*",
                  "key" : "availability"
                }, {
                  "ref" : "system-vc",
                  "@type" : "koral:docGroupRef"
                } ],
                "rewrites" : [ {
                  "@type" : "koral:rewrite",
                  "src" : "Kustvakt",
                  "editor" : "Kustvakt",
                  "operation" : "operation:override",
                  "original" : {
                    "ref" : "system-vc",
                    "@type" : "koral:docGroupRef"
                  },
                  "_comment" : "Free corpus access policy has been added."
                } ]
              },
              "@context" : "http://korap.ids-mannheim.de/ns/koral/0.3/context.jsonld"
            }
            	""";
            
            JsonNode expectedNode = JsonUtils.readTree(expectedJson);
            assertNotNull(node);
            assertEquals(expectedNode, node);
    }

    @Test
    public void testMetaQuerySerialization () throws KustvaktException {
        Response response = target().path(API_VERSION).path("serialize")
                .queryParam("context", "sentence").queryParam("count", "20")
                .queryParam("page", "5").queryParam("cutoff", "true")
                .queryParam("q", "[pos=ADJA]").queryParam("ql", "poliqarp")
                .request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
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
    public void testMetaQuerySerializationWithOffset ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("serialize")
                .queryParam("context", "sentence").queryParam("count", "20")
                .queryParam("page", "5").queryParam("offset", "2")
                .queryParam("cutoff", "true").queryParam("q", "[pos=ADJA]")
                .queryParam("ql", "poliqarp").request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals("sentence", node.at("/meta/context").asText());
        assertEquals(20, node.at("/meta/count").asInt());
        assertEquals(2, node.at("/meta/startIndex").asInt());
        assertEquals(true, node.at("/meta/cutOff").asBoolean());
    }
    
    @Disabled("outdated")
    @Test
    public void testQuerySerializationWithNewCollection ()
            throws KustvaktException {
        // Add Virtual Collection
        Response response = target().path(API_VERSION).path("virtualcollection")
                .queryParam("filter", "false")
                .queryParam("query",
                        "creationDate since 1775 & corpusSigle=GOE")
                .queryParam("name", "Weimarer Werke")
                .queryParam("description", "Goethe-Werke in Weimar (seit 1775)")
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .post(Entity.json(""));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals("Weimarer Werke", node.path("name").asText());
        // Get virtual collections
        response = target().path(API_VERSION).path("collection").request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        ent = response.readEntity(String.class);
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
        response = target().path(API_VERSION).path("collection").path(id)
                .path("serialize").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "base/s:s")
                .request()
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue("kustvakt",
                                        "kustvakt2015"))
                .method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        ent = response.readEntity(String.class);
        node = JsonUtils.readTree(ent);
        assertNotNull(node);
        // System.out.println("NODE " + ent);
        assertEquals("koral:docGroup", node.at(CORPUS_PATH+"/@type").asText());
        assertEquals("koral:doc",
            node.at(CORPUS_PATH+"/operands/0/@type").asText());
        assertEquals("creationDate",
            node.at(CORPUS_PATH+"/operands/0/key").asText());
        assertEquals("1775", node.at(CORPUS_PATH+"/operands/0/value").asText());
        assertEquals("type:date",
            node.at(CORPUS_PATH+"/operands/0/type").asText());
        assertEquals("match:geq",
            node.at(CORPUS_PATH+"/operands/0/match").asText());
        assertEquals("koral:doc",
            node.at(CORPUS_PATH+"/operands/1/@type").asText());
        assertEquals("corpusSigle",
            node.at(CORPUS_PATH+"/operands/1/key").asText());
        assertEquals("GOE", node.at(CORPUS_PATH+"/operands/1/value").asText());
        assertEquals("match:eq",
            node.at(CORPUS_PATH+"/operands/1/match").asText());
    }
}
