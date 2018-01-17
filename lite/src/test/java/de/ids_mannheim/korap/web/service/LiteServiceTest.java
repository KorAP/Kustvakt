package de.ids_mannheim.korap.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.lucene.LucenePackage;
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
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * Created by hanl on 29.04.16.
 * 
 * @author margaretha
 * @date 17/01/2017
 * 
 * Recent changes:
 * - removed test configuration using FastJerseyLightTest
 */
public class LiteServiceTest extends JerseyTest{

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
        return ThreadLocalRandom.current().nextInt(5000, 8000 + 1);
    }
    
    @Test
    public void testStatistics () throws KustvaktException{
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("collectionQuery", "textType=Autobiographie & corpusSigle=GOE")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertEquals(9, node.at("/documents").asInt());
        assertEquals(527662, node.at("/tokens").asInt());
        assertEquals(19387, node.at("/sentences").asInt());
        assertEquals(514, node.at("/paragraphs").asInt());
    }

    @Test
    public void testGetJSONQuery () throws KustvaktException{
        ClientResponse response = resource()
                .path("query").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp").queryParam("context", "sentence")
                .queryParam("count", "13")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);
//        System.out.println(query);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertEquals("opennlp", node.at("/query/wrap/foundry").asText());
        assertEquals("sentence", node.at("/meta/context").asText());
        assertEquals("13", node.at("/meta/count").asText());
    }


    @Test
    public void testbuildAndPostQuery () throws KustvaktException{
        ClientResponse response = resource()
                .path("query").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp")
                .queryParam("cq", "corpusSigle=WPD | corpusSigle=GOE")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        String query = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);

        response = resource().path("search")
                .post(ClientResponse.class, query);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String matches = response.getEntity(String.class);
        JsonNode match_node = JsonUtils.readTree(matches);
        assertNotEquals(0, match_node.path("matches").size());
    }


    @Test
    public void testQueryGet () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp").queryParam("context", "sentence")
                .queryParam("count", "13").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertEquals("base/s:s", node.at("/meta/context").asText());
        assertEquals("13", node.at("/meta/count").asText());
        assertNotEquals(0, node.at("/matches").size());
    }


    @Test
    public void testFoundryRewrite () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp").queryParam("context", "sentence")
                .queryParam("count", "13").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertEquals("opennlp", node.at("/query/wrap/foundry").asText());
    }


    @Test
    public void testQueryPost () throws KustvaktException{
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=das]", "poliqarp");

        ClientResponse response = resource()
                .path("search").post(ClientResponse.class, s.toJSON());
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertNotEquals(0, node.at("/matches").size());
    }


    @Test
    public void testParameterField () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp")
                .queryParam("fields", "author, docSigle")
                .queryParam("context", "sentence").queryParam("count", "13")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertNotEquals(0, node.at("/matches").size());
        assertEquals("[\"author, docSigle\"]", node.at("/meta/fields")
                .toString());
    }

	@Test
	public void testMatchInfoGetWithoutSpans () throws KustvaktException{
        ClientResponse response = resource()
			
			.path("corpus/GOE/AGA/01784/p36-46/matchInfo")
			.queryParam("foundry", "*")
			.queryParam("spans", "false")
			.get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
					 response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("GOE/AGA/01784", node.at("/textSigle").asText());
        assertEquals("Belagerung von Mainz", node.at("/title").asText());
	};

	@Test
	public void testMatchInfoGet2 () throws KustvaktException{
        ClientResponse response = resource()
			
			.path("corpus/GOE/AGA/01784/p36-46/matchInfo")
			.queryParam("foundry", "*")
			.get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
					 response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("GOE/AGA/01784", node.at("/textSigle").asText());
        assertEquals("Belagerung von Mainz", node.at("/title").asText());
	};

    @Test
    public void testCollectionQueryParameter () throws KustvaktException{
        ClientResponse response = resource()
                .path("query").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp")
                .queryParam("fields", "author, docSigle")
                .queryParam("context", "sentence").queryParam("count", "13")
                .queryParam("cq", "textClass=Politik & corpus=WPD")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertEquals("Politik", node.at("/collection/operands/0/value")
                .asText());
        assertEquals("WPD", node.at("/collection/operands/1/value").asText());

        response = resource().path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("fields", "author, docSigle")
                .queryParam("context", "sentence").queryParam("count", "13")
                .queryParam("cq", "textClass=Politik & corpus=WPD")
                .get(ClientResponse.class);
        String version = LucenePackage.get().getImplementationVersion();;
//        System.out.println("VERSION "+ version);
//        System.out.println("RESPONSE "+ response);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        query = response.getEntity(String.class);
        node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertEquals("Politik", node.at("/collection/operands/0/value")
                .asText());
        assertEquals("WPD", node.at("/collection/operands/1/value").asText());
    }

}
