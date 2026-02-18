package de.ids_mannheim.korap.web.lite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.ids_mannheim.korap.config.LiteJerseyTest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/** 
 * Verifies unauthenticated requests are not rate-limited.
 * 
 * Implemented with AI assistance
 */
public class RateLimitAnonymousTest extends LiteJerseyTest {

    @Test
    public void testUnauthenticatedNotRateLimited () {
        // No Authorization header: should remain unauthenticated and not be limited.
        for (int i = 0; i < 80; i++) {
            Response r = target().path(API_VERSION).path("search")
                    .queryParam("q", "[orth=das]")
                    .queryParam("ql", "poliqarp")
                    .request().get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus(),
                    "request " + i);
            r.close();
        }
    }
}