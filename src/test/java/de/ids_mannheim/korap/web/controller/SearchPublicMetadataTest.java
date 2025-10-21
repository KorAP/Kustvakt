package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.controller.vc.VirtualCorpusTestBase;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class SearchPublicMetadataTest extends VirtualCorpusTestBase {

	private double apiVersion = Double.parseDouble(API_VERSION.substring(1));

    @Test
    public void testSearchPublicMetadata () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("access-rewrite-disabled", "true").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertTrue(node.at("/meta/snippet").isMissingNode());
        assertEquals(allCorpusAccess,
        		node.at(CORPUS_PATH+"/rewrites/0/_comment").asText());
        assertFalse(node.at("/matches/0/snippet").asBoolean());
        assertFalse(node.at("/matches/0/tokens").asBoolean());
    }

    @Test
    public void testSearchPublicMetadataExtern () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("access-rewrite-disabled", "true").request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("All corpus access policy has been added.",
        		node.at(CORPUS_PATH+"/rewrites/0/_comment").asText());
        assertTrue(node.at("/matches/0/snippet").isMissingNode());
    }

    @Test
    public void testSearchPublicMetadataWithCustomFields ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("fields", "author,title")
                .queryParam("access-rewrite-disabled", "true").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
		assertEquals(allCorpusAccess,
				node.at(CORPUS_PATH+"/rewrites/0/_comment").asText());
        assertTrue(node.at("/matches/0/snippet").isMissingNode());
        assertEquals("Goethe, Johann Wolfgang von",
            node.at("/matches/0/author").asText());
        assertEquals("Italienische Reise",
            node.at("/matches/0/title").asText());
        // assertEquals(3, node.at("/matches/0").size());
    }

    @Test
    public void testSearchPublicMetadataWithNonPublicField ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("fields", "author,title,snippet")
                .queryParam("access-rewrite-disabled", "true").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.NON_PUBLIC_FIELD_IGNORED,
                node.at("/warnings/0/0").asInt());
        assertEquals("The requested non public fields are ignored",
            node.at("/warnings/0/1").asText());
        assertEquals("snippet", node.at("/warnings/0/2").asText());
    }

    // EM: The API is disabled
    @Disabled
    @Test
    public void testSearchPostPublicMetadata () throws KustvaktException {
        QuerySerializer s = new QuerySerializer(apiVersion);
        s.setQuery("[orth=der]", "poliqarp");
        s.setCollection("corpusSigle=GOE");
        s.setQuery("Wasser", "poliqarp");
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("snippets", "true");
        s.setMeta(meta);
        Response response = target().path(API_VERSION).path("search").request()
                .post(Entity.json(s.toJSON()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(allCorpusAccess,
        		node.at(CORPUS_PATH+"/rewrites/0/_comment").asText());
        assertTrue(node.at("/matches/0/snippet").isMissingNode());
    }

    @Test
    public void testSearchPublicMetadataWithSystemVC ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo system-vc")
                .queryParam("access-rewrite-disabled", "true").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity).at(CORPUS_PATH+"");
        assertEquals("operation:and",node.at("/operation").asText());
        
        assertEquals("operation:override", node.at("/rewrites/0/operation").asText());
        assertEquals("koral:docGroupRef",
                node.at("/rewrites/0/original/@type").asText());
        assertEquals("system-vc", node.at("/rewrites/0/original/ref").asText());
        
        node = node.at("/operands/1");
        assertEquals("koral:docGroupRef", node.at("/@type").asText());
        assertEquals("system-vc", node.at("/ref").asText());
//        assertEquals("GOE", node.at("/value").asText());
//        assertEquals("match:eq", node.at("/match").asText());
//        assertEquals("corpusSigle", node.at("/key").asText());
        
    }

    @Test
    public void testSearchPublicMetadataWithPrivateVC ()
            throws KustvaktException {
    	createDoryVC();
    	createDoryGroupVC();
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"dory/dory-vc\"")
                .queryParam("access-rewrite-disabled", "true").request().get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("guest", node.at("/errors/0/2").asText());
        deleteVC("dory-vc", "dory", "dory");
        deleteVC("group-vc", "dory", "dory");
    }
}
