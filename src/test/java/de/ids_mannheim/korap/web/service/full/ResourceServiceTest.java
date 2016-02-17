package de.ids_mannheim.korap.web.service.full;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author hanl
 * @date 14/01/2016
 */
public class ResourceServiceTest extends FastJerseyTest {

    @BeforeClass
    public static void configure() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
        TestHelper.runBootInterfaces();
    }

    @AfterClass
    public static void close() throws KustvaktException {
        BeanConfiguration.getBeans().getResourceProvider().deleteAll();
        BeanConfiguration.closeApplication();
    }

    @Test
    public void testSearchSimple() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp")
                //                .queryParam("cq", "corpusID=GOE")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assert node.path("matches").size() > 0;
    }

    @Test
    public void testCollectionGet() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection").header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));

        assert node.size() > 0;
    }

    @Test
    @Ignore
    public void testStats() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection").header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assert node != null;

        System.out.println("-------------------------------");
        System.out.println("NODE COLLECTIONS" + node);
        String id = node.path(0).path("id").asText();

        System.out.println("ID IS " + id);
        System.out.println("FROM NODE " + node);
        response = resource().path(getAPIVersion()).path("collection").path(id)
                .path("stats").header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);

        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
        node = JsonUtils.readTree(response.getEntity(String.class));
        assert node != null;
        int docs = node.path("documents").asInt();
        System.out.println("-------------------------------");
        System.out.println("NODE " + node);
        assert docs > 0 && docs < 15;
    }

    @Test
    public void testResourceStore() {

    }

}
