package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.collections.map.LinkedMap;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jwt.SignedJWT;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.config.JWTSigner;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/** EM: To do: not implemented in the new DB yet
 * @author hanl
 * @date 21/03/2015
 */

// todo: do benchmarks for simple request to check access_token check and user
// retrieval!
@Ignore
public class UserServiceTest extends FastJerseyTest {

	private static String[] credentials;

	@Override
	public void initMethod() throws KustvaktException {
		helper().setupAccount();
	}

	@BeforeClass
	public static void setup() throws Exception {
		credentials = new String[2];
		credentials[0] = (String) TestHelper.getUserCredentials().get(Attributes.USERNAME);
		credentials[1] = (String) TestHelper.getUserCredentials().get(Attributes.PASSWORD);
	}

	@Test
	public void testRegisterMissingFields() {

		MultivaluedMap map = new MultivaluedMapImpl();
		map.putSingle("username", "testuser"); // bodmer funktioniert noch nicht
		map.putSingle("email", "hanl@ids-mannheim.de");
		map.putSingle("password", "testPassword1234");
		map.putSingle("firstName", "test");
		map.putSingle("lastName", "user");

		ClientResponse response = resource().path("user").path("register")
				.header("Content-Type", MediaType.APPLICATION_JSON).post(ClientResponse.class, JsonUtils.toJSON(map));
		assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		String ent = response.getEntity(String.class);
		JsonNode node = JsonUtils.readTree(ent);
		assertNotNull(node);
		assertTrue(node.at("/errors/0/2").asText().contains("address"));

		// map.putSingle("address", "Mannheim");

		String enc = BasicHttpAuth.encode("testuser", "testPassword1234");
		response = resource().path("user").path("info")
				.header("Content-Type", MediaType.APPLICATION_JSON).header(Attributes.AUTHORIZATION, enc)
				.get(ClientResponse.class);

		assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}

	// test if user locked and what error message you get back
	@Test
	public void testregisterWithoutConfirm() {
		MultivaluedMap map = new MultivaluedMapImpl();
		map.putSingle("username", "testuser2");
		map.putSingle("email", "hanl@ids-mannheim.de");
		map.putSingle("password", "testPassword1234");
		map.putSingle("firstName", "test");
		map.putSingle("lastName", "user");
		map.putSingle("address", "Mannheim");

		ClientResponse response = resource().path("user").path("register")
				.header("Content-Type", MediaType.APPLICATION_JSON).post(ClientResponse.class, map);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		// run login/ status --> exception or information about locked account
		// should appear
		String enc = BasicHttpAuth.encode("testuser2", "testPassword1234");
		response = resource().path("user").path("info").header(Attributes.AUTHORIZATION, enc)
				.get(ClientResponse.class);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
		String ent = response.getEntity(String.class);

	}

	// should be run over https, since password is transmitted in plain text
	@Test
	public void testRegisterAndConfirm() {
		MultivaluedMap map = new MultivaluedMapImpl();
		map.putSingle("username", "testuser");
		map.putSingle("email", "hanl@ids-mannheim.de");
		map.putSingle("password", "testPassword1234");
		map.putSingle("firstName", "test");
		map.putSingle("lastName", "user");
		map.putSingle("address", "Mannheim");

		ClientResponse response = resource().path("user").path("register")
				.header("Content-Type", MediaType.APPLICATION_JSON).post(ClientResponse.class, map);

		String ent = response.getEntity(String.class);

		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		JsonNode node = JsonUtils.readTree(ent);
		assertNotNull(node);

		String conf_uri = node.path("confirm_uri").asText();
		response = resource().uri(URI.create(conf_uri)).get(ClientResponse.class);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		String enc = BasicHttpAuth.encode("testuser", "testPassword1234");
		response = resource().path("user").path("info").header(Attributes.AUTHORIZATION, enc)
				.get(ClientResponse.class);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
	}

	@Test
	public void loginHTTP() {
		String enc = BasicHttpAuth.encode(credentials[0], credentials[1]);
		ClientResponse response = resource().path("user").path("info")
				.header(Attributes.AUTHORIZATION, enc).get(ClientResponse.class);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
	}

	// EM: This test require VPN / IDS Intranet
	@Test
	@Ignore
	public void loginJWT() {
		String en = BasicHttpAuth.encode(credentials[0], credentials[1]);
		/* lauffähige Version von Hanl: */
		ClientResponse response = resource().path("auth").path("apiToken")
				.header(Attributes.AUTHORIZATION, en).get(ClientResponse.class);
		/**/
		/*
		 * Test : ClientResponse response = null; WebResource webRes =
		 * resource().path("auth") .path("apiToken");
		 * webRes.header(Attributes.AUTHORIZATION, en);
		 * 
		 * System.out.printf("resource: " + webRes.toString());
		 * 
		 * response = webRes.get(ClientResponse.class);
		 * 
		 */

//		assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		String entity = response.getEntity(String.class);
		System.out.println(entity);
		JsonNode node = JsonUtils.readTree(entity);
		assertEquals(2022, node.at("/errors/0/0").asInt());
	}

	// EM: cannot do test with LDAP
	@Test
	@Ignore
	public void loginJWTExpired() throws InterruptedException, KustvaktException, ParseException {

		assertTrue(BeansFactory.getKustvaktContext().getConfiguration().getTokenTTL() < 10);

		String en = BasicHttpAuth.encode(credentials[0], credentials[1]);
		ClientResponse response = resource().path("auth").path("apiToken")
				.header(Attributes.AUTHORIZATION, en).get(ClientResponse.class);

		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
		assertNotNull(node);
		String token = node.path("token").asText();

		JWTSigner sign = new JWTSigner(BeansFactory.getKustvaktContext().getConfiguration().getSharedSecret(),
				BeansFactory.getKustvaktContext().getConfiguration().getIssuer(), -1);

		SignedJWT jwt = sign.verifyToken(token);

		while (true) {
			if (TimeUtils.isExpired(jwt.getJWTClaimsSet().getExpirationTimeClaim()))
				break;
		}

		response = resource().path("user").path("info")
				.header(Attributes.AUTHORIZATION, "api_token " + token).get(ClientResponse.class);
		assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

	}

	@Test
	public void testGetUserDetails() {
		String enc = BasicHttpAuth.encode(credentials[0], credentials[1]);
		ClientResponse response = resource().path("user").path("details")
				.header(Attributes.AUTHORIZATION, enc).get(ClientResponse.class);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
	}

	@Test
	public void testGetUserDetailsEmbeddedPointer() {
		String enc = BasicHttpAuth.encode(credentials[0], credentials[1]);
		Map m = new LinkedMap();
		m.put("test", "[100, \"error message\", true, \"another message\"]");

		ClientResponse response = resource().path("user").path("details")
				.header(Attributes.AUTHORIZATION, enc).header("Content-Type", MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, m);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		response = resource().path("user").path("details").queryParam("pointer", "test")
				.header(Attributes.AUTHORIZATION, enc).get(ClientResponse.class);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
		String ent = response.getEntity(String.class);
		assertEquals("[100, \"error message\", true, \"another message\"]", ent);
	}

	@Test
	public void testUpdateUserDetailsMerge() {
		String enc = BasicHttpAuth.encode(credentials[0], credentials[1]);
		Map m = new LinkedMap();
		m.put("test", "test value 1");

		ClientResponse response = resource().path("user").path("details")
				.header(Attributes.AUTHORIZATION, enc).header("Content-Type", MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, m);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		response = resource().path("user").path("details").header(Attributes.AUTHORIZATION, enc)
				.get(ClientResponse.class);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
		String ent = response.getEntity(String.class);
		JsonNode node = JsonUtils.readTree(ent);
		assertNotNull(node);
		assertEquals("test value 1", node.at("/test").asText());
		assertEquals("user", node.at("/lastName").asText());
		assertEquals("test@ids-mannheim.de", node.at("/email").asText());
	}

	@Test
	public void testGetUserDetailsPointer() {
		String enc = BasicHttpAuth.encode(credentials[0], credentials[1]);
		ClientResponse response = resource().path("user").path("details")
				.queryParam("pointer", "email").header(Attributes.AUTHORIZATION, enc).get(ClientResponse.class);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
		String ent = response.getEntity(String.class);
		assertEquals("test@ids-mannheim.de", ent);
	}

	@Test
	public void testGetUserDetailsNonExistent() throws KustvaktException {
		helper().setupSimpleAccount("userservicetest", "servicepass");

		String enc = BasicHttpAuth.encode("userservicetest", "servicepass");
		ClientResponse response = resource().path("user").path("details")
				.header(Attributes.AUTHORIZATION, enc).get(ClientResponse.class);
		assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		String entity = response.getEntity(String.class);
		JsonNode node = JsonUtils.readTree(entity);
		assertNotNull(node);
		assertEquals(StatusCodes.NO_RESULT_FOUND, node.at("/errors/0/0").asInt());
		assertEquals("UserDetails", node.at("/errors/0/2").asText());
		helper().dropUser("userservicetest");
	}

	@Test
	public void testGetUserSettings() {
		String enc = BasicHttpAuth.encode(credentials[0], credentials[1]);
		ClientResponse response = resource().path("user").path("settings")
				.header(Attributes.AUTHORIZATION, enc).get(ClientResponse.class);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
	}

	@Test
	public void testUpdateUserDetailsJson() {
		String enc = BasicHttpAuth.encode(credentials[0], credentials[1]);
		Map m = new LinkedMap();
		m.put("firstName", "newName");
		m.put("lastName", "newLastName");
		m.put("email", "newtest@ids-mannheim.de");

		ClientResponse response = resource().path("user").path("details")
				.header(Attributes.AUTHORIZATION, enc).header("Content-Type", MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, m);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		response = resource().path("user").path("details").header(Attributes.AUTHORIZATION, enc)
				.get(ClientResponse.class);

		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
		JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
		assertNotNull(node);
		assertEquals("newName", node.path("firstName").asText());
		assertEquals("newLastName", node.path("lastName").asText());
		assertEquals("newtest@ids-mannheim.de", node.path("email").asText());
		assertEquals("Mannheim", node.path("address").asText());

		m = new LinkedMap();
		m.put("firstName", "test");
		m.put("lastName", "user");
		m.put("email", "test@ids-mannheim.de");

		response = resource().path("user").path("details").header(Attributes.AUTHORIZATION, enc)
				.header("Content-Type", MediaType.APPLICATION_JSON).post(ClientResponse.class, m);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
	}

	@Test
	@Ignore
	public void testUpdateUserSettingsForm() throws IOException {
		String enc = BasicHttpAuth.encode(credentials[0], credentials[1]);
		MultivaluedMap m = new MultivaluedMapImpl();
		m.putSingle("queryLanguage", "poliqarp_test");
		m.putSingle("pageLength", "200");

		ClientResponse response = resource().path("user").path("settings")
				.header(Attributes.AUTHORIZATION, enc).header("Content-Type", "application/x-www-form-urlencoded")
				.get(ClientResponse.class);

		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		JsonNode map = JsonUtils.readTree(response.getEntity(String.class));
		assertNotNull(map);

		assertNotEquals(m.getFirst("queryLanguage"), map.get("queryLanguage"));
		assertNotEquals(m.get("pageLength"), Integer.valueOf((String) m.getFirst("pageLength")));

		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		response = resource().path("user").path("settings").header(Attributes.AUTHORIZATION, enc)
				.header("Content-Type", "application/x-www-form-urlencoded").post(ClientResponse.class, m);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		response = resource().path("user").path("settings").header(Attributes.AUTHORIZATION, enc)
				.header("Content-Type", "application/x-www-form-urlencoded").get(ClientResponse.class);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		map = JsonUtils.readTree(response.getEntity(String.class));
		assertNotNull(map);

		assertEquals(map.get("queryLanguage"), m.getFirst("queryLanguage"));
		int p1 = map.path("pageLength").asInt();
		int p2 = Integer.valueOf((String) m.getFirst("pageLength"));
		assertEquals(p1, p2);
	}

	@Test
	public void testUpdateUserSettingsJson() throws IOException {
		String enc = BasicHttpAuth.encode(credentials[0], credentials[1]);
		Map m = new HashMap<>();
		m.put("queryLanguage", "poliqarp_test");
		m.put("pageLength", "200");
		m.put("setting_1", "value_1");

		ClientResponse response = resource().path("user").path("settings")
				.header(Attributes.AUTHORIZATION, enc).header("Content-Type", MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);

		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		JsonNode map = JsonUtils.readTree(response.getEntity(String.class));
		assertNotNull(map);

		assertNotEquals(m.get("queryLanguage"), map.get("queryLanguage"));
		assertNotEquals(m.get("pageLength"), Integer.valueOf((String) m.get("pageLength")));

		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		response = resource().path("user").path("settings").header(Attributes.AUTHORIZATION, enc)
				.header("Content-Type", MediaType.APPLICATION_JSON).post(ClientResponse.class, m);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		response = resource().path("user").path("settings").header(Attributes.AUTHORIZATION, enc)
				.get(ClientResponse.class);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());

		map = JsonUtils.readTree(response.getEntity(String.class));
		assertNotNull(map);

		assertEquals(map.path("queryLanguage").asText(), m.get("queryLanguage"));
		int p1 = map.path("pageLength").asInt();
		int p2 = Integer.valueOf((String) m.get("pageLength"));
		assertEquals(p1, p2);
	}

	@Test
	public void registerInvalidPassword() {
		final String CREDENTIALS_INVALID = "{\"username\":\"testuser2\","
				+ "\"email\":\"hanl@ids-mannheim.de\",\"password\":\"testpassword\"}";
		// Response response = given()
		// .contentType("application/x-www-form-urlencoded").when()
		// .body(CREDENTIALS_INVALID).post("/register");
		// String body = response.getBody().asString();
		// Assert.assertEquals("response is in error", true,
		// body.contains("error"));
		// JsonNode n = JsonUtils.readTree(body);
		// String uri = n.path("uri").asText();
		// Response r = given().get(URI.create(uri));
		// System.out.println("RESPONSE " + r.getStatusCode());
		// System.out.println("RESPONSE BODY " + n);
	}

	@Test
	public void testLoginFailedLockAccount() {

	}

	@Test
	@Ignore
	public void delete() {

	}

	@Test
	@Ignore
	public void updatePassword() {

	}

}
