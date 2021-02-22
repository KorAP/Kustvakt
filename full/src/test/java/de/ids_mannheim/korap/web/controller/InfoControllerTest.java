package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.web.SearchKrill;

public class InfoControllerTest extends SpringJerseyTest {

    @Autowired
    private KustvaktConfiguration config;
    @Autowired
    private SearchKrill krill;

    @Test
    public void testInfo () throws KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("info")
                .get(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(config.getCurrentVersion(),
                node.at("/latest_api_version").asText());
        assertEquals(config.getSupportedVersions().size(),
                node.at("/supported_api_versions").size());

        assertEquals(ServiceInfo.getInfo().getVersion(),
                node.at("/kustvakt_version").asText());
        assertEquals(krill.getIndex().getVersion(),
                node.at("/krill_version").asText());
        assertEquals(ServiceInfo.getInfo().getKoralVersion(),
                node.at("/koral_version").asText());
    }
}
