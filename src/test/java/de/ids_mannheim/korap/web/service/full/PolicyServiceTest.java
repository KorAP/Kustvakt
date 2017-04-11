package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.PolicyHandlerIface;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.Permissions.Permission;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.security.PolicyCondition;
import de.ids_mannheim.korap.security.SecurityPolicy;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.UserFactory;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/**
 * @author margaretha
 */
public class PolicyServiceTest extends FastJerseyTest {

	@BeforeClass
	public static void configure() throws Exception {
		FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.full", "de.ids_mannheim.korap.web.filter",
				"de.ids_mannheim.korap.web.utils");
	}

	@Test
    public void testCreatePolicyForResource() throws IOException, KustvaktException {
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
