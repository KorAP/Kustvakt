package de.ids_mannheim.korap.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.util.KrillProperties;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Isolated
@ContextConfiguration(
		locations = "classpath:test-config-dnb.xml", 
		inheritLocations = false
)		
public class DNBTest extends SpringJerseyTest {

    public final static String API_VERSION = "v1.1";

    private JsonNode sendQuery (String query) throws KustvaktException {
        Response r = target().path(API_VERSION).path("search")
                .queryParam("q", query).queryParam("ql", "poliqarp")
                .queryParam("show-tokens", true).request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        String entity = r.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        return node;

    }
    
    @BeforeAll
    public static void loadDnbProperties() {
        KrillProperties.loadProperties("kustvakt-dnb.conf");
    }

    @AfterAll
    public static void resetKrillProperties() {
        KrillProperties.loadProperties("kustvakt-test.conf");
    }

	@Test
	public void testTokenMatchSize () throws KustvaktException {
		assertEquals(12, KrillProperties.maxTokenMatchSize);
		assertEquals(25, KrillProperties.maxTokenContextSize);

		JsonNode node = sendQuery("[orth=das]");
		assertEquals(1, node.at("/matches/0/tokens/match").size());

		node = sendQuery("[orth=das][orth=Glück]");
		assertEquals(6, node.at("/matches/0/context/left/1").asInt());
		assertEquals(6, node.at("/matches/0/context/right/1").asInt());
	}

    @Test
    public void testTokenContextMatchSize () throws KustvaktException {
        Response r = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das][orth=Glück]")
                .queryParam("ql", "poliqarp").queryParam("show-tokens", true)
                .queryParam("context", "30-token,30-token").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        String entity = r.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(KrillProperties.maxTokenContextSize,
                node.at("/meta/context/left/1").asInt());
        assertEquals(KrillProperties.maxTokenContextSize,
                node.at("/meta/context/right/1").asInt());
        
        assertEquals(24, // using kwic
                node.at("/matches/0/tokens/left").size());
        
        assertEquals(24,
                node.at("/matches/0/tokens/right").size());
    }

}
