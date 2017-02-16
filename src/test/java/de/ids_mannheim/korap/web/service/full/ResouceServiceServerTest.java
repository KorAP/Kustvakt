package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.ResourceDao;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * This class tests services of a running Kustvakt server with a MySQL database. 
 * Please check the database configuration in src/main/resources/jdbc.properties 
 * and run the server before running the tests.
 * 
 * See {@link ResourceServiceTest} for tests using an in-memory database.
 * 
 * @author margaretha
 *
 */
public class ResouceServiceServerTest extends BeanConfigTest{

	@Test
	public void testWrongAuthorization() throws IOException, URISyntaxException {
		HttpResponse response = testResourceStore("wezrowerowj");
		assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(), response.getStatusLine().getStatusCode());
	}

	@Test
	public void testCorrectAuthorization() throws IOException, URISyntaxException, KustvaktException {

		HttpResponse response = testResourceStore("kustvakt2015");
		HttpEntity entity = response.getEntity();
		String content = null;

		if (entity != null) {
			InputStream is = entity.getContent();
			try {
				content = IOUtils.toString(is, "UTF-8");
			} finally {
				is.close();
			}
		}

		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());

		JsonNode node = JsonUtils.readTree(content);
		assertNotNull(node);
		assertTrue(node.isObject());
		assertEquals("Goethe", node.path("name").asText());
		assertEquals("Goethe corpus", node.path("description").asText());
		
		//checkResourceInDB(node.path("id").asText());
	}

	public HttpResponse testResourceStore(String password) throws IOException, URISyntaxException {

		HttpClient httpclient = HttpClients.createDefault();
		URIBuilder builder = new URIBuilder();
		builder.setScheme("http").setHost("localhost").setPort(8089).setPath("/api/v0.1/virtualcollection")
				.setParameter("filter", "httpclient").setParameter("name", "Goethe")
				.setParameter("description", "Goethe corpus");
		URI uri = builder.build();
		HttpPost httppost = new HttpPost(uri);
		httppost.addHeader(Attributes.AUTHORIZATION, BasicHttpAuth.encode("kustvakt", password));
		return httpclient.execute(httppost);

	}
	
	private void checkResourceInDB(String id) throws KustvaktException {
		
		ResourceDao<?> dao = new ResourceDao<>(helper().getContext()
                .getPersistenceClient());
        assertEquals("sqlite", helper().getContext().getPersistenceClient()
                .getDatabase());

        assertNotEquals(0, dao.size());
        KustvaktResource res = dao.findbyId(id,
                User.UserFactory.getDemoUser());
        assertNotNull(res);
        Assert.assertEquals(true,res.getField("testVar").toString().startsWith("testVal_"));
	}

	@Override
	public void initMethod() throws KustvaktException {
		// TODO Auto-generated method stub
		
	}
}
