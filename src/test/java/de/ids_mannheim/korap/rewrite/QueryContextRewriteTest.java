package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.MetaQueryBuilder;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class QueryContextRewriteTest extends SpringJerseyTest {
    
    @Autowired
    public RewriteHandler rewriteHandler;
    
    @Autowired
    private KustvaktConfiguration config;

    private static final String LARGE_CONTEXT_GROUP = "LargeContextGroup";
    private static final String LARGE_CONTEXT_GROUP_ADMIN = "korap_admin";
    private static final String TEST_USER = "largeContextTestUser";

    @Test
    public void testCutTokenContext () throws KustvaktException, Exception {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne")
                .queryParam("ql", "poliqarp")
                .queryParam("context", "60-token,60-token")
                .request()
                .get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        
        JsonNode context = node.at("/meta/context");
        assertEquals(config.getMaxTokenContext(), context.at("/left/1").asInt());
        assertEquals(config.getMaxTokenContext(), context.at("/right/1").asInt());
        
        // match context
        context = node.at("/matches/0/context");
        assertEquals(config.getMaxTokenContext(), context.at("/left/1").asInt());
        assertEquals(config.getMaxTokenContext(), context.at("/right/1").asInt());
    }
    
    /** AI generated
     * 
     * When large.context.group.enabled = true, a member of the
     * LargeContextGroup must receive the larger maxTokenContextLarge limit.
     * @throws KustvaktExceptiona
     */
    @Test
    public void testLargeContextEnabledForGroupMember ()
            throws KustvaktException {
        // add test user to LargeContextGroup via web service
        Form form = new Form();
        form.param("members", TEST_USER);
        Response addResponse = target().path(API_VERSION).path("group")
                .path("@" + LARGE_CONTEXT_GROUP).path("member").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(
                                LARGE_CONTEXT_GROUP_ADMIN, "pass"))
                .put(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), addResponse.getStatus());

        try {
            Response searchResponse = target().path(API_VERSION).path("search")
                    .queryParam("q", "Sonne")
                    .queryParam("ql", "poliqarp")
                    .queryParam("context", "60-token,60-token")
                    .request()
                    .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                            .createBasicAuthorizationHeaderValue(
                                    TEST_USER, "pass"))
                    .get();
            String ent = searchResponse.readEntity(String.class);
            JsonNode node = JsonUtils.readTree(ent);

            JsonNode context = node.at("/meta/context");
            assertEquals(config.getMaxTokenContextLarge(),
                    context.at("/left/1").asInt());
            assertEquals(config.getMaxTokenContextLarge(),
                    context.at("/right/1").asInt());

            // match context
            context = node.at("/matches/0/context");
            assertEquals(config.getMaxTokenContextLarge(),
                    context.at("/left/1").asInt());
            assertEquals(config.getMaxTokenContextLarge(),
                    context.at("/right/1").asInt());
        }
        finally {
            // remove test user from group via web service
            Response deleteResponse = target().path(API_VERSION).path("group")
                    .path("@" + LARGE_CONTEXT_GROUP)
                    .path("~" + TEST_USER).request()
                    .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                            .createBasicAuthorizationHeaderValue(
                                    LARGE_CONTEXT_GROUP_ADMIN, "pass"))
                    .delete();
            assertEquals(Status.OK.getStatusCode(), deleteResponse.getStatus());
        }
    }



    /**
     * When large.context.group.enabled = true, a user who is NOT a
     * member of the LargeContextGroup must still be capped at the
     * regular maxTokenContext limit.
     */
    @Test
    public void testLargeContextEnabledForNonMember ()
            throws KustvaktException {
        config.setLargeContextGroupEnabled(true);
        // TEST_USER is not added to the group here
        Response searchResponse = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne")
                .queryParam("ql", "poliqarp")
                .queryParam("context", "60-token,60-token")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(
                                TEST_USER, "pass"))
                .get();
        String ent = searchResponse.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);

        // non-member must receive the regular limit
        JsonNode context = node.at("/meta/context");
        assertEquals(config.getMaxTokenContext(),
                context.at("/left/1").asInt());
        assertEquals(config.getMaxTokenContext(),
                context.at("/right/1").asInt());
    }

    /**
     * When large.context.group.enabled = false, a member of the
     * LargeContextGroup must still be capped at the regular
     * maxTokenContext limit.
     */
    @Test
    public void testLargeContextDisabledForGroupMember ()
            throws KustvaktException {
        // add test user to LargeContextGroup
        Form form = new Form();
        form.param("members", TEST_USER);
        Response addResponse = target().path(API_VERSION).path("group")
                .path("@" + LARGE_CONTEXT_GROUP).path("member").request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(
                                LARGE_CONTEXT_GROUP_ADMIN, "pass"))
                .put(Entity.form(form));
        assertEquals(Status.OK.getStatusCode(), addResponse.getStatus());

        // disable the feature flag
        config.setLargeContextGroupEnabled(false);
        try {
            Response searchResponse = target().path(API_VERSION).path("search")
                    .queryParam("q", "Sonne")
                    .queryParam("ql", "poliqarp")
                    .queryParam("context", "60-token,60-token")
                    .request()
                    .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                            .createBasicAuthorizationHeaderValue(
                                    TEST_USER, "pass"))
                    .get();
            String ent = searchResponse.readEntity(String.class);
            JsonNode node = JsonUtils.readTree(ent);

            // must be capped at regular limit, NOT the large limit
            JsonNode context = node.at("/meta/context");
            assertEquals(config.getMaxTokenContext(),
                    context.at("/left/1").asInt());
            assertEquals(config.getMaxTokenContext(),
                    context.at("/right/1").asInt());
        }
        finally {
            // restore feature flag
            config.setLargeContextGroupEnabled(true);
            // remove test user from group
            Response deleteResponse = target().path(API_VERSION).path("group")
                    .path("@" + LARGE_CONTEXT_GROUP)
                    .path("~" + TEST_USER).request()
                    .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                            .createBasicAuthorizationHeaderValue(
                                    LARGE_CONTEXT_GROUP_ADMIN, "pass"))
                    .delete();
            assertEquals(Status.OK.getStatusCode(), deleteResponse.getStatus());
        }
    }

    /**
     * When large.context.group.enabled = false, an anonymous (null)
     * user must also be capped at the regular maxTokenContext.
     */
    @Test
    public void testLargeContextDisabledForAnonymousUser ()
            throws KustvaktException {
        config.setLargeContextGroupEnabled(false);
        try {
            Response response = target().path(API_VERSION).path("search")
                    .queryParam("q", "Sonne")
                    .queryParam("ql", "poliqarp")
                    .queryParam("context", "60-token,60-token")
                    .request()
                    .get();
            String ent = response.readEntity(String.class);
            JsonNode node = JsonUtils.readTree(ent);

            JsonNode context = node.at("/meta/context");
            assertEquals(config.getMaxTokenContext(),
                    context.at("/left/1").asInt());
            assertEquals(config.getMaxTokenContext(),
                    context.at("/right/1").asInt());
        }
        finally {
            config.setLargeContextGroupEnabled(true);
        }
    }

    @Test
    public void testMetaRewrite () throws KustvaktException {
        QuerySerializer s = new QuerySerializer(API_VERSION_DOUBLE);
        s.setQuery("Schnee within s", "poliqarp");
        
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.setSpanContext("60-token,60-token");
        s.setMeta(meta.raw());
        
        String jsonQuery = s.toJSON();
        JsonNode queryNode = JsonUtils.readTree(jsonQuery);
        
        JsonNode context = queryNode.at("/meta/context");
        assertEquals(60, context.at("/left/1").asInt());
        assertEquals(60, context.at("/right/1").asInt());
        
        String result = rewriteHandler.processQuery(s.toJSON(), 
        		new KorAPUser("test"), API_VERSION_DOUBLE);
        JsonNode node = JsonUtils.readTree(result);
        
        context = node.at("/meta/context");
        assertEquals(40, context.at("/left/1").asInt());
        assertEquals(40, context.at("/right/1").asInt());
        
        assertEquals("koral:rewrite", context.at("/rewrites/0/@type").asText());
        assertEquals("Kustvakt", context.at("/rewrites/0/editor").asText());
        assertEquals("operation:override", context.at("/rewrites/0/operation").asText());
        assertEquals("left", context.at("/rewrites/0/scope").asText());
        assertEquals("token", context.at("/rewrites/0/original/0").asText());
        assertEquals(60, context.at("/rewrites/0/original/1").asInt());
        
        assertEquals("right", context.at("/rewrites/1/scope").asText());
        assertEquals("token", context.at("/rewrites/1/original/0").asText());
        assertEquals(60, context.at("/rewrites/1/original/1").asInt());
    }

    
}
