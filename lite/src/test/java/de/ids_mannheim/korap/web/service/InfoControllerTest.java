package de.ids_mannheim.korap.web.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.LiteJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.web.SearchKrill;

public class InfoControllerTest extends LiteJerseyTest {

    @Autowired
    private KustvaktConfiguration config;

    @Autowired
    private SearchKrill krill;
    
    @Test
    public void testInfo () throws KustvaktException {
        Response response = target().path(API_VERSION).path("info")
                .request()
                .get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.readEntity(String.class);
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
