package de.ids_mannheim.korap.scenario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * <p>Test scenario for ICC (International Comparable Corpus)
 * instance</p>
 * 
 * 
 * The instance requires user authentication and access to data is
 * restricted to only logged-in users.
 * 
 * This class uses <em>test-config-icc.xml</em> spring XML config
 * defining the location of a specific kustvakt configuration file for
 * this instance:<em>kustvakt-icc.conf</em>.
 * 
 * When running a Kustvakt jar file, these files must be included in
 * the classpath. In the example below, the files are placed together
 * in the a folder named <em>config</em> and it is included in the
 * classpath. Besides, <em>jdbc.properties</em> is required at the
 * same folder as the jar.
 * 
 * <p>
 * <code>
 * java -cp Kustvakt-full-0.69.3.jar:config
 * de.ids_mannheim.korap.server.KustvaktServer --spring-config
 * test-config-icc.xml
 * </code>
 * </p>
 * 
 * <p>
 * For ICC, collectionRewrite in the Spring XML configuration must
 * be disabled. This has been done in <em>test-config-icc.xml</em>.
 * </p>
 * 
 * The configuration file: <em>kustvakt-icc.conf</em> includes the
 * following setup:
 * 
 * <ul>
 * <li>
 * <em>availability.regex</em>
 * properties should be removed or commented since the data doesn't
 * contain availability and access to data is not determined by this
 * field.
 * </li>
 * <li>
 * Resource filter class names for the search and match info services
 * should be defined by <em>search.resource.filters property</em>. For
 * example:
 * 
 * <p>
 * <code>search.resource.filters=AuthenticationFilter </code>
 * </p>
 * 
 * restricts access with authentication only.
 * </li>
 * </ul>
 * 
 * @author elma
 * @see /src/main/resources/properties/jdbc.properties
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
        assertTrue(node.at("/matches").size() > 0);
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
