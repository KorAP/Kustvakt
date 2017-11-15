package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.framework.HttpAuthorizationHandler;
import de.ids_mannheim.korap.authentication.framework.TransferEncoding;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.AuthenticationType;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.ResourceDao;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions.Permission;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * This class tests services of a running Kustvakt server with a MySQL
 * database.
 * Please check the database configuration in
 * src/main/resources/jdbc.properties
 * and run the server before running the tests.
 * 
 * See {@link ResourceServiceTest} for tests using an in-memory
 * database.
 * 
 * @author margaretha
 *
 */
@Ignore
@Deprecated
public class KustvaktServerTest extends BeanConfigTest {
    private static ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private HttpAuthorizationHandler handler;


    @Test
    public void testRegisterBadPassword ()
            throws URISyntaxException, ClientProtocolException, IOException, KustvaktException {
        HttpClient httpClient = HttpClients.createDefault();

        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.putSingle("username", "kusvakt");
        map.putSingle("email", "kustvakt@ids-mannheim.de");
        map.putSingle("password", "password");
        map.putSingle("firstName", "kustvakt");
        map.putSingle("lastName", "user");
        map.putSingle("address", "Mannheim");

        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost("localhost").setPort(8089)
                .setPath("/api/v0.1/user/register");
        URI uri = builder.build();
        HttpPost httppost = new HttpPost(uri);
        StringEntity entity = new StringEntity(JsonUtils.toJSON(map));
        httppost.setEntity(entity);
        httppost.addHeader(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON);
        httppost.addHeader(HttpHeaders.USER_AGENT, "Apache HTTP Client");
        httppost.addHeader(HttpHeaders.HOST, "localhost");

        HttpResponse response = httpClient.execute(httppost);
        assertEquals(ClientResponse.Status.NOT_ACCEPTABLE.getStatusCode(),
                response.getStatusLine().getStatusCode());

        HttpEntity responseEntity = response.getEntity();
        JsonNode errorNode = mapper.readTree(responseEntity.getContent());
        assertEquals(
                "[The value for the parameter password is not valid or acceptable.]",
                errorNode.get("errors").get(0).get(2).asText());

    }


    @Test
    public void testRegisterExistingUsername ()
            throws URISyntaxException, ClientProtocolException, IOException, KustvaktException {
        HttpClient httpClient = HttpClients.createDefault();

        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.putSingle("username", "kustvakt");
        map.putSingle("email", "kustvakt@ids-mannheim.de");
        map.putSingle("password", "password1234");
        map.putSingle("firstName", "kustvakt");
        map.putSingle("lastName", "user");
        map.putSingle("address", "Mannheim");

        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost("localhost").setPort(8089)
                .setPath("/api/v0.1/user/register");
        URI uri = builder.build();
        HttpPost httppost = new HttpPost(uri);
        StringEntity entity = new StringEntity(JsonUtils.toJSON(map));
        httppost.setEntity(entity);
        httppost.addHeader(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON);
        httppost.addHeader(HttpHeaders.USER_AGENT, "Apache HTTP Client");
        httppost.addHeader(HttpHeaders.HOST, "localhost");

        HttpResponse response = httpClient.execute(httppost);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatusLine().getStatusCode());

        HttpEntity responseEntity = response.getEntity();
        JsonNode errorNode = mapper.readTree(responseEntity.getContent());
        assertEquals(
                "[The value for the parameter password is not valid or acceptable.]",
                errorNode.get("errors").asText());
    }


    @Test
    public void testRegisterUser ()
            throws URISyntaxException, ClientProtocolException, IOException, KustvaktException {
        HttpClient httpClient = HttpClients.createDefault();

        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.putSingle("username", "testUser");
        map.putSingle("email", "testUser@ids-mannheim.de");
        map.putSingle("password", "testPassword1234");
        map.putSingle("firstName", "test");
        map.putSingle("lastName", "user");
        map.putSingle("address", "Mannheim");

        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost("localhost").setPort(8089)
                .setPath("/api/v0.1/user/register");
        URI uri = builder.build();
        HttpPost httppost = new HttpPost(uri);
        StringEntity entity = new StringEntity(JsonUtils.toJSON(map));
        httppost.setEntity(entity);
        httppost.addHeader(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON);
        httppost.addHeader(HttpHeaders.USER_AGENT, "Apache HTTP Client");
        httppost.addHeader(HttpHeaders.HOST, "localhost");

        HttpResponse response = httpClient.execute(httppost);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatusLine().getStatusCode());

    }


    @Test
    public void testCreatePolicy () throws IOException, URISyntaxException, KustvaktException {

        HttpClient httpClient = HttpClients.createDefault();

        String id = UUID.randomUUID().toString();
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost("localhost").setPort(8089)
                .setPath("/api/v0.1/admin/createPolicies/" + id)
                .setParameter("type", "virtualcollection")
                .setParameter("name", "Goethe VC")
                .setParameter("description", "Goethe corpus")
                .setParameter("group", "public")
                .setParameter("perm", Permission.READ.name())
                .setParameter("loc", "").setParameter("expire", "");

        URI uri = builder.build();
        HttpPost httppost = new HttpPost(uri);

        httppost.addHeader(Attributes.AUTHORIZATION,
                handler.createAuthorizationHeader(AuthenticationType.BASIC,"kustvakt", "kustvakt2015"));
        HttpResponse response = httpClient.execute(httppost);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatusLine().getStatusCode());

    }


    @Test
    public void testCreatePolicyForFoundry ()
            throws IOException, URISyntaxException, KustvaktException {

        HttpClient httpClient = HttpClients.createDefault();

        String id = UUID.randomUUID().toString();
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost("localhost").setPort(8089)
                .setPath("/api/v0.1/admin/createPolicies/" + id)
                .setParameter("type", "foundry")
                .setParameter("name", "stanford")
                .setParameter("description", "stanford parser")
                .setParameter("group", "public")
                .setParameter("perm", Permission.READ.name())
                .setParameter("loc", "255.255.255.0")
                .setParameter("expire", "30D");

        URI uri = builder.build();
        HttpPost httppost = new HttpPost(uri);

        httppost.addHeader(Attributes.AUTHORIZATION,
                handler.createAuthorizationHeader(AuthenticationType.BASIC,"kustvakt", "kustvakt2015"));
        HttpResponse response = httpClient.execute(httppost);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatusLine().getStatusCode());

    }


    @Test
    public void testCreatePolicyWithMultiplePermissions ()
            throws IOException, URISyntaxException, KustvaktException {

        HttpClient httpClient = HttpClients.createDefault();

        String id = UUID.randomUUID().toString();
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost("localhost").setPort(8089)
                .setPath("/api/v0.1/admin/createPolicies/" + id)
                .setParameter("type", "corpus").setParameter("name", "Brown")
                .setParameter("description", "Brown corpus")
                .setParameter("group", "public")
                .setParameter("perm", Permission.READ.name())
                .setParameter("perm", Permission.WRITE.name())
                .setParameter("perm", Permission.DELETE.name())
                .setParameter("loc", "255.255.255.0")
                .setParameter("expire", "30D");

        URI uri = builder.build();
        HttpPost httppost = new HttpPost(uri);

        httppost.addHeader(Attributes.AUTHORIZATION,
                handler.createAuthorizationHeader(AuthenticationType.BASIC,"kustvakt", "kustvakt2015"));
        HttpResponse response = httpClient.execute(httppost);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatusLine().getStatusCode());

    }


    @Test
    public void testWrongAuthorization ()
            throws IOException, URISyntaxException, KustvaktException {
        HttpResponse response = testResourceStore("wezrowerowj");
        assertEquals(ClientResponse.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatusLine().getStatusCode());
    }


    @Test
    public void testCorrectAuthorization ()
            throws IOException, URISyntaxException, KustvaktException {

        HttpResponse response = testResourceStore("kustvakt2015");
        HttpEntity entity = response.getEntity();
        String content = null;

        if (entity != null) {
            InputStream is = entity.getContent();
            try {
                content = IOUtils.toString(is, "UTF-8");
            }
            finally {
                is.close();
            }
        }

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatusLine().getStatusCode());

        JsonNode node = JsonUtils.readTree(content);
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals("Goethe", node.path("name").asText());
        assertEquals("Goethe corpus", node.path("description").asText());
    }


    public HttpResponse testResourceStore (String password)
            throws IOException, URISyntaxException, KustvaktException {

        HttpClient httpclient = HttpClients.createDefault();
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost("localhost").setPort(8089)
                .setPath("/api/v0.1/virtualcollection")
                .setParameter("filter", "httpclient")
                .setParameter("name", "Goethe")
                .setParameter("description", "Goethe corpus");
        URI uri = builder.build();
        HttpPost httppost = new HttpPost(uri);
        httppost.addHeader(Attributes.AUTHORIZATION,
                handler.createAuthorizationHeader(AuthenticationType.BASIC,"kustvakt", password));
        return httpclient.execute(httppost);

    }
    
    @Test
    public void testResourceUpdate ()
            throws IOException, URISyntaxException, KustvaktException {

        HttpClient httpclient = HttpClients.createDefault();
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost("localhost").setPort(8089)
                //.setPath("/api/v0.1/virtualcollection/00df953b-2227-4c23-84c1-5532c07bf8ce")
                .setPath("/api/v0.1/virtualcollection/GOE-VC")
                .setParameter("name", "Goethe collection")
                .setParameter("description", "Goethe collection");
        URI uri = builder.build();
        HttpPost httppost = new HttpPost(uri);
        httppost.addHeader(Attributes.AUTHORIZATION,
                handler.createAuthorizationHeader(AuthenticationType.BASIC,"kustvakt", "kustvakt2015"));
        HttpResponse response = httpclient.execute(httppost);
        
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatusLine().getStatusCode());
        
//        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
//                response.getStatusLine().getStatusCode());
        
//        JsonNode errorNode = mapper.readTree(response.getEntity().getContent());
//        assertEquals(
//                "Resource not found!",
//                errorNode.get("errors").get(0).get(1).asText());
        
    }


    private void checkResourceInDB (String id) throws KustvaktException {

        ResourceDao<?> dao = new ResourceDao<>(
                helper().getContext().getPersistenceClient());
        assertEquals("sqlite",
                helper().getContext().getPersistenceClient().getDatabase());

        assertNotEquals(0, dao.size());
        KustvaktResource res = dao.findbyId(id, User.UserFactory.getDemoUser());
        assertNotNull(res);
        Assert.assertEquals(true,
                res.getField("testVar").toString().startsWith("testVal_"));
    }


    @Override
    public void initMethod () throws KustvaktException {
        // TODO Auto-generated method stub

    }
}
