package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

@ContextConfiguration("classpath:test-resource-config.xml")
public class ResourceControllerTest extends SpringJerseyTest {

	@Test
	public void testResource () throws KustvaktException {
		Response response = target().path(API_VERSION).path("resource")
				.request().get();
		String entity = response.readEntity(String.class);
		JsonNode n = JsonUtils.readTree(entity);
		assertEquals(3, n.size());

		n = n.get(0);
		assertEquals("http://hdl.handle.net/10932/00-03B6-558F-4E10-6201-1",
				n.at("/resourceId").asText());
		assertEquals(n.at("/titles/de").asText(),
				"Deutsche Wikipedia Artikel 2017");
		assertEquals(n.at("/titles/en").asText(),
				"German Wikipedia Articles 2017");
		assertEquals(1, n.at("/languages").size());
		assertEquals(6, n.at("/layers").size());
		assertEquals("IDS Mannheim", n.at("/institution").asText());
		assertEquals("https://korap.ids-mannheim.de?cq=corpusSigle=WPD17",
				n.at("/landingPage").asText());
		assertEquals("free", n.at("/requiredAccess").asText());
	}
}
