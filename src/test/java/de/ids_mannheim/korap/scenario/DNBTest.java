package de.ids_mannheim.korap.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.util.KrillProperties;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@ContextConfiguration(
		locations = "classpath:test-config-dnb.xml", 
		inheritLocations = false
)		
public class DNBTest extends SpringJerseyTest {

    public final static String API_VERSION = "v1.0";

    private JsonNode sendQuery (String query) throws KustvaktException {
        Response r = target().path(API_VERSION).path("search")
                .queryParam("q", query).queryParam("ql", "poliqarp")
                .queryParam("show-tokens", true).request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        String entity = r.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        return node;

    }
    
    @AfterAll
    public static void resetKrillProperties() {
        KrillProperties.loadProperties("kustvakt-test.conf");
    }

    @Test
    public void testTokenMatchSize () throws KustvaktException {
        assertEquals(1, KrillProperties.maxTokenMatchSize);
        assertEquals(25, KrillProperties.maxTokenContextSize);

        JsonNode node = sendQuery("[orth=das]");
        assertEquals(KrillProperties.maxTokenMatchSize,
                node.at("/matches/0/tokens/match").size());

        node = sendQuery("[orth=das][orth=Glück]");
        assertEquals(KrillProperties.maxTokenMatchSize,
                node.at("/matches/0/tokens/match").size());
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
        
        assertEquals(KrillProperties.maxTokenContextSize,
                node.at("/matches/0/tokens/left").size());
        
        // There is a bug in Krill (https://github.com/KorAP/Krill/issues/141)
        // So the following test fails
//        assertEquals(KrillProperties.maxTokenContextSize,
//                node.at("/matches/0/tokens/right").size());
    }

}
