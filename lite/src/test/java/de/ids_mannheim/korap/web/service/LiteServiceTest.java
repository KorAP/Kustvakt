package de.ids_mannheim.korap.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Iterator;
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
 * @author margaretha, diewald
 * @date 08/03/2018
 * 
 * Recent changes:
 * - removed test configuration using FastJerseyLightTest
 * - added metadata test
 * - updated field type:date 
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
    public void testStatistics () throws KustvaktException{
        ClientResponse response = resource()
                .path("statistics")
                .queryParam("corpusQuery", "textType=Autobiographie & corpusSigle=GOE")
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
    public void testEmptyStatistics () throws KustvaktException{
        ClientResponse response = resource()
			.path("statistics")
			.queryParam("corpusQuery", "")
			.method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertEquals(11, node.at("/documents").asInt());
        assertEquals(665842, node.at("/tokens").asInt());
        assertEquals(25074, node.at("/sentences").asInt());
        assertEquals(772, node.at("/paragraphs").asInt());

		response = resource()
                .path("statistics")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
		query = response.getEntity(String.class);
		node = JsonUtils.readTree(query);
        assertEquals(11, node.at("/documents").asInt());
        assertEquals(665842, node.at("/tokens").asInt());
        assertEquals(25074, node.at("/sentences").asInt());
        assertEquals(772, node.at("/paragraphs").asInt());
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
    public void testQueryFailure () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=das")
                .queryParam("ql", "poliqarp")
                .queryParam("cq", "corpusSigle=WPD | corpusSigle=GOE")
			.queryParam("count", "13")
			.get(ClientResponse.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String query = response.getEntity(String.class);

		JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals(302, node.at("/errors/0/0").asInt());
        assertEquals(302, node.at("/errors/1/0").asInt());
		assertTrue(node.at("/errors/2").isMissingNode());
		assertFalse(node.at("/collection").isMissingNode());
        assertEquals(13, node.at("/meta/count").asInt());
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
			.path("corpus/GOE/AGA/01784/p36-46(5)37-45(2)38-42/matchInfo")
			.queryParam("foundry", "*")
			.queryParam("spans", "false")
			.get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
					 response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("GOE/AGA/01784", node.at("/textSigle").asText());
        assertEquals("match-GOE/AGA/01784-p36-46(5)37-45(2)38-42",
					 node.at("/matchID").asText());
        assertEquals("Belagerung von Mainz", node.at("/title").asText());
	};

	@Test
	public void testMatchInfoGetWithoutHighlights () throws KustvaktException{
        ClientResponse response = resource()
			.path("corpus/GOE/AGA/01784/p36-46(5)37-45(2)38-42/matchInfo")
			.queryParam("foundry", "xy")
			.queryParam("spans", "false")
			.get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
					 response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("<span class=\"context-left\"></span><span class=\"match\">der alte freie Weg nach Mainz war gesperrt, ich mußte über die Schiffbrücke bei Rüsselsheim; in Ginsheim ward <mark>gefüttert; der Ort ist sehr zerschossen; dann über die Schiffbrücke</mark> auf die Nonnenaue, wo viele Bäume niedergehauen lagen, sofort auf dem zweiten Teil der Schiffbrücke über den größern Arm des Rheins.</span><span class=\"context-right\"></span>",
					 node.at("/snippet").asText());
        assertEquals("GOE/AGA/01784", node.at("/textSigle").asText());
        assertEquals("match-GOE/AGA/01784-p36-46(5)37-45(2)38-42",
					 node.at("/matchID").asText());
        assertEquals("Belagerung von Mainz", node.at("/title").asText());
	};

	
	@Test
	public void testMatchInfoGetWithHighlights () throws KustvaktException{
        ClientResponse response = resource()			
			.path("corpus/GOE/AGA/01784/p36-46(5)37-45(2)38-42/matchInfo")
			.queryParam("foundry", "xy")
			.queryParam("spans", "false")
			.queryParam("hls", "true")
			.get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
					 response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("GOE/AGA/01784", node.at("/textSigle").asText());
        assertEquals("<span class=\"context-left\"></span><span class=\"match\">der alte freie Weg nach Mainz war gesperrt, ich mußte über die Schiffbrücke bei Rüsselsheim; in Ginsheim ward <mark>gefüttert; <mark class=\"class-5 level-0\">der <mark class=\"class-2 level-1\">Ort ist sehr zerschossen; dann</mark> über die Schiffbrücke</mark></mark> auf die Nonnenaue, wo viele Bäume niedergehauen lagen, sofort auf dem zweiten Teil der Schiffbrücke über den größern Arm des Rheins.</span><span class=\"context-right\"></span>",
					 node.at("/snippet").asText());
		assertEquals("match-GOE/AGA/01784-p36-46(5)37-45(2)38-42",
					 node.at("/matchID").asText());
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

	@Test
	public void testMetaFields () throws KustvaktException {
        ClientResponse response = resource()
                .path("/corpus/GOE/AGA/01784")
                .method("GET", ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String resp = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(resp);
//		System.err.println(node.toString());

		Iterator<JsonNode> fieldIter = node.at("/document/fields").elements();

		int checkC = 0;
		while (fieldIter.hasNext()) {
			JsonNode field = (JsonNode) fieldIter.next();

			String key = field.at("/key").asText();

			assertEquals("koral:field", field.at("/@type").asText());

			switch (key) {
			case "textSigle":
				assertEquals("type:string", field.at("/type").asText());
				assertEquals("GOE/AGA/01784", field.at("/value").asText());
				checkC++;
				break;
			case "author":
				assertEquals("type:text", field.at("/type").asText());
				assertEquals("Goethe, Johann Wolfgang von", field.at("/value").asText());
				checkC++;
				break;
			case "docSigle":
				assertEquals("type:string", field.at("/type").asText());
				assertEquals("GOE/AGA", field.at("/value").asText());
				checkC++;
				break;
			case "docTitle":
				assertEquals("type:text", field.at("/type").asText());
				assertEquals(
					"Goethe: Autobiographische Schriften II, (1817-1825, 1832)",
					field.at("/value").asText()
					);
				checkC++;
				break;
			case "pubDate":
				assertEquals("type:date", field.at("/type").asText());
				assertEquals(1982, field.at("/value").asInt());
				checkC++;
				break;
			};		
		};
		assertEquals(5, checkC);
	};
}
