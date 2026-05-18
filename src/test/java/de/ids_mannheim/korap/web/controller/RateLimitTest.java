package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.controller.oauth2.OAuth2TestBase;
import de.ids_mannheim.korap.web.filter.RateLimitFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**  
 * Verifies authenticated rate limiting (HTTP 429) is applied after
 * auth.
 * 
 * Implemented with AI assistance
 */
@Isolated
public class RateLimitTest extends OAuth2TestBase {
	@Autowired
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    public void clearRateLimitState() {
        // Clear rate limit state before each test
        if (rateLimitFilter != null) {
            rateLimitFilter.clearBuckets();
        }
    }
	@Test
	public void testAuthenticatedRateLimitBearerToken ()
			throws KustvaktException {
		Response response = requestTokenWithDoryPassword(superClientId,
				clientSecret);
		JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
		String accessToken = node.at("/access_token").asText();
		
		for (long i = 0; i < rateLimitFilter.getBurstCapacity(); i++) {
			Response r = searchWithAccessToken(accessToken);
			assertEquals(Status.OK.getStatusCode(), r.getStatus(),
					"request " + i);
			r.close();
		}

		Response limited = searchWithAccessToken(accessToken);
		assertEquals(429, limited.getStatus());
		limited.close();
	}

	@Test
	public void testRateLimitDisabled () throws KustvaktException {
		rateLimitFilter.setEnabled(false);
		try {
			Response response = requestTokenWithDoryPassword(superClientId,
					clientSecret);
			JsonNode node = JsonUtils
					.readTree(response.readEntity(String.class));
			String accessToken = node.at("/access_token").asText();

			// Exceed burst capacity – all requests should still succeed
			long overLimit = rateLimitFilter.getBurstCapacity() + 5;
			for (long i = 0; i < overLimit; i++) {
				Response r = searchWithAccessToken(accessToken);
				assertEquals(Status.OK.getStatusCode(), r.getStatus(),
						"request " + i + " should succeed when rate limiting is disabled");
				r.close();
			}
		}
		finally {
			// Always re-enable so other tests are not affected
			rateLimitFilter.setEnabled(true);
		}
	}
}