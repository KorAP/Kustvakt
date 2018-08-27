package de.ids_mannheim.korap.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;
import org.springframework.web.context.ContextLoaderListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly.web.GrizzlyWebTestContainerFactory;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class VCReferenceTest extends JerseyTest{

    public static final String classPackage = "de.ids_mannheim.korap.web.service.light";
    
    @Override
    protected TestContainerFactory getTestContainerFactory ()
            throws TestContainerException {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    protected AppDescriptor configure () {
        return new WebAppDescriptor.Builder(classPackage)
                .servletClass(SpringServlet.class)
                .contextListenerClass(ContextLoaderListener.class)
                .contextParam("contextConfigLocation",
                        "classpath:lite-config.xml")
                .build();
    }

    @Override
    protected int getPort (int defaultPort) {
        int port = ThreadLocalRandom.current().nextInt(5000, 8000 + 1);
        try {
            ServerSocket socket = new ServerSocket(port);
            socket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            port = getPort(port);
        }
        return port;
    }
    
    @Test
    public void testSearchWithVCRef () throws KustvaktException {
        ClientResponse response = resource().path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .queryParam("cq", "referTo named-vc1")
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertTrue(node.at("/matches").size() > 0);

    }
    
    @Test
    public void testStatisticsWithVCReference () throws KustvaktException {
        String corpusQuery = "referTo named-vc1";
        ClientResponse response = resource().path("statistics")
                .queryParam("corpusQuery", corpusQuery)
                .get(ClientResponse.class);

        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(2, node.at("/documents").asInt());
    }
}
