package de.ids_mannheim.korap.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.KustvaktBaseServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hanl
 * @date 14/01/2016
 */
public class KustvaktResourceServiceTest extends FastJerseyTest {

    @BeforeClass
    public static void configure() {
        BeanConfiguration.loadClasspathContext("default-config.xml");
        setPackages("de.ids_mannheim.korap.web.service.full",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");

        KustvaktBaseServer.runPreStart();
    }

    @AfterClass
    public static void close() {
        BeanConfiguration.closeApplication();
    }

    @Test
    public void testSearchSimple() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("search").queryParam("q", "[base=Wort]")
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
    public void testStats() {
        ClientResponse response = resource().path(getAPIVersion())
                .path("collection").header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assert node != null;
        String id = node.path(0).path("id").asText();

        response = resource().path(getAPIVersion()).path("collection").path(id)
                .path("stats").header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assert ClientResponse.Status.OK.getStatusCode() == response.getStatus();
        node = JsonUtils.readTree(response.getEntity(String.class));
        assert node != null;
        int docs = node.path("documents").asInt();
        assert docs > 0 && docs < 15;
    }

    @Test
    public void testResourceStore() {

    }

}
