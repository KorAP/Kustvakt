package de.ids_mannheim.korap.web.service;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.LiteJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class LiteSearchPipeTest extends LiteJerseyTest {

    @Autowired
    private KustvaktConfiguration config;

    private void setTestPipes () throws IOException {
        String filename = "test-pipes";
        File f = new File(filename);
        if (f.exists()) {
            f.delete();
        }
        f.createNewFile();
        OutputStreamWriter writer =
                new OutputStreamWriter(new FileOutputStream(f));
        writer.append("glemm\t");
        writer.append(resource().getURI().toString());
        writer.append(API_VERSION);
        writer.append("/test/glemm");
        writer.flush();
        writer.close();

        config.readPipesFile(filename);
    }

    @Test
    public void testSearchWithPipes () throws IOException, KustvaktException {
        setTestPipes();
        ClientResponse response = resource().path(API_VERSION).path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("pipes", "glemm").get(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(3, node.at("/query/wrap/key").size());
        node = node.at("/query/wrap/rewrites");
        assertEquals(2, node.size());
        assertEquals("Glemm", node.at("/0/src").asText());
        assertEquals("operation:override", node.at("/0/operation").asText());
        assertEquals("key", node.at("/0/scope").asText());
        
        assertEquals("Kustvakt", node.at("/1/src").asText());
        assertEquals("operation:injection", node.at("/1/operation").asText());
        assertEquals("foundry", node.at("/1/scope").asText());
    }
}
