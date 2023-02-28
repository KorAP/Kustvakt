package de.ids_mannheim.korap.scenario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

/** Test scenario for ICC (International Comparable Corpus) instance
 * 
 *  The instance requires user authentication and access to data is 
 *  restricted to only logged-in users.
 *  
 *  This class uses test-config-icc.xml spring XML config defining 
 *  the location of a specific kustvakt configuration file for this instance:
 *  kustvakt-icc.conf which can be placed in the root project folder or 
 *  in the same folder as the Kustvakt jar file.
 *  
 *  For ICC, collectionRewrite in the Spring XML configuration must 
 *  be disabled.
 *  
 *  In the kustvakt configuration file availability.regex properties 
 *  should be removed or commented since the data doesn't contain 
 *  availability and access to data is not determined by this field.
 *  
 *  Resource filter class names for the search and match info services 
 *  should be defined by search.resource.filters property. For example:
 *  
 *  search.resource.filters=AuthenticationFilter
 *  
 *  restricts access with authentication only.
 * 
 * @author elma
 *
 */
@ContextConfiguration("classpath:test-config-icc.xml")
public class ICCTest extends SpringJerseyTest {

    public final static String API_VERSION = "v1.0";
    public String basicAuth;

    public ICCTest () throws KustvaktException {
        basicAuth = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("user", "password");
    }
    
    @Test
    public void searchWithoutLogin () throws KustvaktException {    
        Response r = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .request().get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());

        String entity = r.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void searchWithLogin () throws KustvaktException {
        Response r = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .request().header(Attributes.AUTHORIZATION, basicAuth).get();

        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        String entity = r.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertTrue(node.at("/matches").size()>0);
    }

    @Test
    public void matchInfoWithoutLogin () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("WDD17").path("982").path("72848").path("p2815-2816")
                .queryParam("foundry", "*").request().get();

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    @Test
    public void matchInfoWithLogin () throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus")
                .path("WDD17").path("982").path("72848").path("p2815-2816")
                .queryParam("foundry", "*").request()
                .header(Attributes.AUTHORIZATION, basicAuth).get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertTrue(node.at("/hasSnippet").asBoolean());
        assertNotNull(node.at("/matchID").asText());
        assertNotNull(node.at("/snippet").asText());
    }
}
