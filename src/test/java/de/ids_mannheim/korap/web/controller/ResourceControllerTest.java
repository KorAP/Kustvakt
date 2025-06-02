package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import jakarta.ws.rs.core.Response;

@ContextConfiguration("classpath:test-resource-config.xml")
public class ResourceControllerTest extends SpringJerseyTest {

	@Test
	public void testResource () throws KustvaktException {
		Response response = target().path(API_VERSION).path("resource")
				.request().get();
		String entity = response.readEntity(String.class);
		JsonNode n = JsonUtils.readTree(entity);
		assertEquals(29, n.size());

		JsonNode n0 = n.get(0);
		assertEquals("http://hdl.handle.net/10932/00-03B6-558F-4E10-6201-1",
				n0.at("/resourceId").asText());
		assertEquals(n0.at("/titles/de").asText(),
				"Deutsche Wikipedia Artikel 2017");
		assertEquals(n0.at("/titles/en").asText(),
				"German Wikipedia Articles 2017");
		assertEquals(1, n0.at("/languages").size());
		assertEquals(6, n0.at("/layers").size());
		assertEquals("IDS Mannheim", n0.at("/institution").asText());
		assertEquals("https://korap.ids-mannheim.de?cq=corpusSigle%3DWPD17",
				n0.at("/landingPage").asText());
		assertEquals("FREE", n0.at("/requiredAccess").asText());
		assertEquals("A collection of articles of German Wikipedia from July "
				+ "1st, 2017.", n0.at("/description").asText());
		
		JsonNode n1 = n.get(1);
		assertEquals("http://hdl.handle.net/10932/00-03B6-558F-5EA0-6301-B",
				n1.at("/resourceId").asText());
		assertEquals(n1.at("/titles/de").asText(),
				"Deutsche Wikipedia-Diskussionskorpus 2017");
		assertEquals(n1.at("/titles/en").asText(),
				"German Wikipedia talk corpus 2017");
		assertEquals(1, n1.at("/languages").size());
		assertEquals(6, n1.at("/layers").size());
		assertEquals("IDS Mannheim", n1.at("/institution").asText());
		assertEquals("https://korap.ids-mannheim.de?cq=corpusSigle%3DWDD17",
				n1.at("/landingPage").asText());
		assertEquals("FREE", n1.at("/requiredAccess").asText());
	
		JsonNode n4 = n.get(4);
		assertEquals("Literatur",
				n4.at("/resourceId").asText());
		assertEquals(n4.at("/titles/de").asText(),
				"Literatur");
		assertEquals(n4.at("/titles/en").asText(),
				"Literature");
		assertEquals(1, n4.at("/languages").size());
		assertEquals(6, n4.at("/layers").size());
		assertEquals("IDS Mannheim", n4.at("/institution").asText());
		assertEquals("https://korap.ids-mannheim.de?cq=textType+%3D+%2F%28.*%5BRr%5Doman%7C%5BBb%5Diographie%7C%5BDd%5Drama%7C%5BSs%5Dschauspiel%29%2F",
				n4.at("/landingPage").asText());
		assertEquals("FREE", n4.at("/requiredAccess").asText());
		assertTrue(n4.at("/description").isMissingNode());
	}
}
