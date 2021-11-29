package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;

public class PublicMetadataTest extends SpringJerseyTest {

    @Test
    public void testSearchPublicMetadata () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("access-rewrite-disabled", "true")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals("availability(ALL)",
                node.at("/collection/rewrites/0/scope").asText());

        assertTrue(node.at("/matches/0/snippet").isMissingNode());
    }

    @Test
    public void testSearchPublicMetadataExtern () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("access-rewrite-disabled", "true")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertEquals("availability(ALL)",
                node.at("/collection/rewrites/0/scope").asText());

        assertTrue(node.at("/matches/0/snippet").isMissingNode());
    }
    
    @Test
    public void testSearchPublicMetadataWithCustomFields () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("fields", "author,title")
                .queryParam("access-rewrite-disabled", "true")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals("availability(ALL)",
                node.at("/collection/rewrites/0/scope").asText());

        assertTrue(node.at("/matches/0/snippet").isMissingNode());
        assertEquals("Goethe, Johann Wolfgang von",
                node.at("/matches/0/author").asText());
        assertEquals("Italienische Reise",
                node.at("/matches/0/title").asText());
//        assertEquals(3, node.at("/matches/0").size());
    }
    
    @Test
    public void testSearchPublicMetadataWithNonPublicField () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("fields", "author,title,snippet")
                .queryParam("access-rewrite-disabled", "true")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(StatusCodes.NON_PUBLIC_FIELD_IGNORED,
                node.at("/warnings/0/0").asInt());
        assertEquals("The requested non public fields are ignored",
                node.at("/warnings/0/1").asText());
        assertEquals("snippet",
                node.at("/warnings/0/2").asText());
    }

//  EM: The API is disabled
    @Ignore
    @Test
    public void testSearchPostPublicMetadata () throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=der]", "poliqarp");
        s.setCollection("corpusSigle=GOE");
        s.setQuery("Wasser", "poliqarp");
        
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("snippets", "true");
        s.setMeta(meta);
        
        ClientResponse response = resource().path(API_VERSION).path("search")
                .post(ClientResponse.class, s.toJSON());
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(ent);
        assertEquals("availability(ALL)",
                node.at("/collection/rewrites/0/scope").asText());
        assertTrue(node.at("/matches/0/snippet").isMissingNode());
    }
    
    @Test
    public void testSearchPublicMetadataWithSystemVC ()
            throws KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo system-vc")
                .queryParam("access-rewrite-disabled", "true")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals("operation:and",
                node.at("/collection/operation").asText());
        node = node.at("/collection/operands/1");
        assertEquals("koral:doc", node.at("/@type").asText());
        assertEquals("GOE", node.at("/value").asText());
        assertEquals("match:eq", node.at("/match").asText());
        assertEquals("corpusSigle", node.at("/key").asText());

        assertEquals("operation:deletion",
                node.at("/rewrites/0/operation").asText());
        assertEquals("@type(koral:docGroupRef)",
                node.at("/rewrites/0/scope").asText());

        assertEquals("operation:deletion",
                node.at("/rewrites/1/operation").asText());
        assertEquals("ref(system-vc)", node.at("/rewrites/1/scope").asText());

        assertEquals("operation:insertion",
                node.at("/rewrites/2/operation").asText());
    }

    @Test
    public void testSearchPublicMetadataWithPrivateVC ()
            throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"dory/dory-vc\"")
                .queryParam("access-rewrite-disabled", "true")
                .get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("guest", node.at("/errors/0/2").asText());
    }
}
