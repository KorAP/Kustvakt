package de.ids_mannheim.korap.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * <p>Test scenario for ICC (International Comparable Corpus)
 * instance</p>
 * <p>
 * The instance requires user authentication and access to data is
 * restricted to only logged-in users.
 * <p>
 * This class uses <em>test-config-icc.xml</em> spring XML config
 * defining the location of a specific kustvakt configuration file for
 * this instance:<em>kustvakt-icc.conf</em>.
 *
 * <p>
 * To run a Kustvakt jar with ICC setup, the following files are needed:
 * </p>
 * <ul>
 * <li>a Spring configuration file</li>
 * <li>a Kustvakt configuration file that must be placed at the jar folder</li>
 * <li>a JDBC properties file that must be placed at the jar folder</li>
 * </ul>
 * <p>
 * Example:
 *
 * <p>
 * <code>
 * java -jar Kustvakt-full-0.69.3.jar --spring-config
 * test-config-icc.xml
 * </code>
 * </p>
 *
 * <h1>Spring configuration file</h1>
 * <p>
 * For ICC, collectionRewrite in the Spring XML configuration must
 * be disabled. This has been done in <em>test-config-icc.xml</em>.
 * </p>
 *
 * <p>For testing, the ICC configuration uses HTTP Basic
 * Authentication and doesn't use LDAP.</p>
 *
 * <p>For production, Basic Authentication must be
 * disabled/commented.</p>
 *
 * <pre><code>
 * &lt;bean id="basic_auth"
 * class="de.ids_mannheim.korap.authentication.BasicAuthentication"/&gt;
 *
 * &lt;util:list id="kustvakt_authproviders"
 * value-type="de.ids_mannheim.korap.interfaces.AuthenticationIface"&gt;
 * &lt;!-- &lt;ref bean="basic_auth" /&gt; --&gt;
 * </code>
 * </pre>
 *
 * <p>For production, the init-method of Initializator should be changed to init.</p>
 *
 * <pre>
 * <code>
 * &lt;bean id="initializator" class="de.ids_mannheim.de.init.Initializator"
 *   init-method="init"&gt;&lt;/bean&gt;
 * </code>
 * </pre>
 *
 * <h1>Kustvakt configuration file</h1>
 *
 * <p>
 * The configuration file: <em>kustvakt-icc.conf</em> includes the
 * following setup:
 * </p>
 *
 * <ul>
 * <li>
 * <em>krill.indexDir</em> should indicate the location of the index.
 * It is set to the wiki-index for the test.
 * </li>
 *
 * <p>
 * <code>krill.indexDir=../wiki-index</code>
 * </p>
 *
 * <li>
 * <em>availability.regex</em>
 * properties should be removed or commented since the data doesn't
 * contain availability and access to data is not determined by this
 * field.
 * </li>
 *
 * <li>
 * Resource filter class names for the search and match info services
 * should be defined by <em>search.resource.filters property</em>. For
 * example, to restricts access with only authentication filter:</li>
 *
 * <p>
 * <code>search.resource.filters=AuthenticationFilter </code>
 * </p>
 *
 * <li><em>oauth2.password.authentication</em> indicating the
 * authentication method to match usernames and password.
 * <code>TEST</code> is a dummy authentication that doesn't do any
 * matching. For production, it must be changed to
 * <code>LDAP</code>.</li>
 *
 * <p><code>oauth2.password.authentication=LDAP</code></p>
 *
 * </ul>
 *
 * @author elma
 * @see /src/main/resources/properties/jdbc.properties
 */
@ContextConfiguration("classpath:test-config-icc.xml")
public class ICCTest extends SpringJerseyTest {

    public final static String API_VERSION = "v1.0";

    public String basicAuth;

    public ICCTest() throws KustvaktException {
        basicAuth = HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("user", "password");
    }

    @Test
    public void searchWithoutLogin() throws KustvaktException {
        Response r = target().path(API_VERSION).path("search").queryParam("q", "[orth=das]").queryParam("ql", "poliqarp").request().get();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
        String entity = r.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED, node.at("/errors/0/0").asInt());
    }

    @Test
    public void searchWithLogin() throws KustvaktException {
        Response r = target().path(API_VERSION).path("search").queryParam("q", "[orth=das]").queryParam("ql", "poliqarp").request().header(Attributes.AUTHORIZATION, basicAuth).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        String entity = r.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertTrue(node.at("/matches").size() > 0);
    }

    @Test
    public void matchInfoWithoutLogin() throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus").path("WDD17").path("982").path("72848").path("p2815-2816").queryParam("foundry", "*").request().get();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED, node.at("/errors/0/0").asInt());
    }

    @Test
    public void matchInfoWithLogin() throws KustvaktException {
        Response response = target().path(API_VERSION).path("corpus").path("WDD17").path("982").path("72848").path("p2815-2816").queryParam("foundry", "*").request().header(Attributes.AUTHORIZATION, basicAuth).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertTrue(node.at("/hasSnippet").asBoolean());
        assertNotNull(node.at("/matchID").asText());
        assertNotNull(node.at("/snippet").asText());
    }
}
