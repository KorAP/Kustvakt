package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import jakarta.ws.rs.core.Response;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;

public class SearchPublicMetadataTest extends SpringJerseyTest {

    @Test
    public void testSearchPublicMetadata () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("access-rewrite-disabled", "true").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/collection/rewrites/0/scope").asText(),
                "availability(ALL)");
        assertTrue(node.at("/matches/0/snippet").isMissingNode());
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
        assertEquals(node.at("/collection/rewrites/0/scope").asText(),
                "availability(ALL)");
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
        assertEquals(node.at("/collection/rewrites/0/scope").asText(),
                "availability(ALL)");
        assertTrue(node.at("/matches/0/snippet").isMissingNode());
        assertEquals(node.at("/matches/0/author").asText(),
                "Goethe, Johann Wolfgang von");
        assertEquals(node.at("/matches/0/title").asText(),
                "Italienische Reise");
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
        assertEquals(node.at("/warnings/0/1").asText(),
                "The requested non public fields are ignored");
        assertEquals(node.at("/warnings/0/2").asText(), "snippet");
    }

    // EM: The API is disabled
    @Disabled
    @Test
    public void testSearchPostPublicMetadata () throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
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
        assertEquals(node.at("/collection/rewrites/0/scope").asText(),
                "availability(ALL)");
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
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(node.at("/collection/operation").asText(),
                "operation:and");
        node = node.at("/collection/operands/1");
        assertEquals(node.at("/@type").asText(), "koral:doc");
        assertEquals(node.at("/value").asText(), "GOE");
        assertEquals(node.at("/match").asText(), "match:eq");
        assertEquals(node.at("/key").asText(), "corpusSigle");
        assertEquals(node.at("/rewrites/0/operation").asText(),
                "operation:deletion");
        assertEquals(node.at("/rewrites/0/scope").asText(), "@type");
        assertEquals(node.at("/rewrites/0/source").asText(),
                "koral:docGroupRef");
        
        assertEquals(node.at("/rewrites/1/operation").asText(),
                "operation:deletion");
        assertEquals(node.at("/rewrites/1/scope").asText(), "ref");
        assertEquals(node.at("/rewrites/1/source").asText(), "system-vc");
        
        
        assertEquals(node.at("/rewrites/2/operation").asText(),
                "operation:injection");
    }

    @Test
    public void testSearchPublicMetadataWithPrivateVC ()
            throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo \"dory/dory-vc\"")
                .queryParam("access-rewrite-disabled", "true").request().get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/2").asText(), "guest");
    }
}
