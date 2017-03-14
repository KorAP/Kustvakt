package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

import de.ids_mannheim.korap.config.AdminSetup;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.ResourceDao;
import de.ids_mannheim.korap.interfaces.db.PolicyHandlerIface;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.Permissions.Permission;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.security.PolicyCondition;
import de.ids_mannheim.korap.security.SecurityPolicy;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.UserFactory;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/**
 * @author margaretha
 */
public class PolicyServiceTest extends FastJerseyTest {

	@BeforeClass
	public static void configure() throws Exception {
		FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full", "de.ids_mannheim.korap.web.filter",
				"de.ids_mannheim.korap.web.utils");
		// containerURI = "https://localhost/";
	}

//	public void initServer(int port) {
//		super.initServer(port);
//
//		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
//			public X509Certificate[] getAcceptedIssuers() {
//				return null;
//			}
//
//			public void checkClientTrusted(X509Certificate[] certs, String authType) {
//			}
//
//			public void checkServerTrusted(X509Certificate[] certs, String authType) {
//			}
//		} };
//
//		HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
//		ClientConfig config = new DefaultClientConfig();
//		SSLContext ctx = null;
//		try {
//			ctx = SSLContext.getInstance("SSL");
//			ctx.init(null, trustAllCerts, new java.security.SecureRandom());
//		} catch (NoSuchAlgorithmException | KeyManagementException e) {
//			e.printStackTrace();
//		}
//
//		config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
//				new HTTPSProperties(hostnameVerifier, ctx));
//		client = Client.create(config);
//		
//		AdminSetup.getInstance();
//	}

	@Test
    public void testCreatePolicyForResource() throws IOException, KustvaktException {
    	//Path p = FileSystems.getDefault().getPath("admin_token");
    	//List<String> content = Files.readAllLines(p, StandardCharsets.UTF_8);
    	//String adminToken = content.get(0);
		String id = UUID.randomUUID().toString();
    	ClientResponse response = resource()
                .path(getAPIVersion())
                .path("admin")
                .path("createPolicies")
                .path(id)
                .queryParam("type", "virtualcollection")
                .queryParam("name", "Goethe VC")
                .queryParam("description", "Goethe corpus")
                .queryParam("group", "public")
                .queryParam("perm", Permission.READ.name())
                .queryParam("loc", "")
                .queryParam("expire", "")
                .header(Attributes.AUTHORIZATION,
                        BasicHttpAuth.encode("kustvakt","kustvakt2015"))
                .post(ClientResponse.class);
        
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        
        // Check the policies
        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        List<SecurityPolicy> policies = dao.getPolicies(
                new PolicyCondition("public"), VirtualCollection.class,
                Permissions.Permission.READ.toByte());
        assertEquals(2, policies.size());
        
        // Check resource store
        List<ResourceOperationIface> providers= (List<ResourceOperationIface>) helper().getContext().getResourceProviders();
        ResourceOperationIface resourceDao = providers.get(0);
        
        User user = UserFactory.getDemoUser();
		KustvaktResource resource = resourceDao.findbyId(id,user);
		assertEquals("Goethe VC", resource.getName());
        	
	}

	@Override
	public void initMethod() throws KustvaktException {
		helper().runBootInterfaces();
	}
}
