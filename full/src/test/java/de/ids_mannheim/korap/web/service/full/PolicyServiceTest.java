package de.ids_mannheim.korap.web.service.full;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.AuthenticationType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.PolicyHandlerIface;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.Foundry;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.Permissions.Permission;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.security.PolicyCondition;
import de.ids_mannheim.korap.security.SecurityPolicy;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.User.UserFactory;
import de.ids_mannheim.korap.web.service.FastJerseyTest;

/** FIX ME: Database restructure
 * @author margaretha
 */
@Ignore
public class PolicyServiceTest extends FastJerseyTest {

    @Autowired
    HttpAuthorizationHandler handler;
    
    private User user = UserFactory.getDemoUser();


    @Test
    public void testCreatePolicyForVirtualCollection ()
            throws IOException, KustvaktException {
        String id = UUID.randomUUID().toString();
        ClientResponse response = resource().path(getAPIVersion()).path("admin")
                .path("createPolicies").path(id)
                .queryParam("type", "virtualcollection")
                .queryParam("name", "Goethe VC")
                .queryParam("description", "Goethe corpus")
                .queryParam("group", "public")
                .queryParam("perm", Permission.READ.name())
                .queryParam("expire", "")
                .header(Attributes.AUTHORIZATION,
                        handler.createAuthorizationHeader(AuthenticationType.BASIC,"kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        // Check the policies
        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        List<SecurityPolicy> policies = dao.getPolicies(
                new PolicyCondition("public"), VirtualCollection.class,
                Permissions.Permission.READ.toByte());
        assertEquals(2, policies.size());
        
        policies = dao.getPoliciesByPersistentId(
                new PolicyCondition("public"), VirtualCollection.class,
                Permissions.Permission.READ.toByte(),id);
        assertEquals(1, policies.size());
        assertEquals(id, policies.get(0).getTarget());

        // Check the resource
        List<ResourceOperationIface> providers = (List<ResourceOperationIface>) helper()
                .getContext().getResourceProviders();
        ResourceOperationIface resourceDao = providers.get(0);

        User user = UserFactory.getDemoUser();
        KustvaktResource resource = resourceDao.findbyId(id, user);
        assertEquals("Goethe VC", resource.getName());

    }


    @Test
    public void testCreatePolicyForFoundry ()
            throws IOException, KustvaktException {
        String id = UUID.randomUUID().toString();
        ClientResponse response = resource().path(getAPIVersion()).path("admin")
                .path("createPolicies").path(id).queryParam("type", "foundry")
                .queryParam("name", "stanford")
                .queryParam("description", "stanford parser")
                .queryParam("group", "public")
                .queryParam("perm", Permission.READ.name())
                .queryParam("loc", "255.255.255.0")
                .queryParam("expire", "30D")
                .header(Attributes.AUTHORIZATION,
                        handler.createAuthorizationHeader(AuthenticationType.BASIC,"kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        // Check the resource store
        List<ResourceOperationIface> providers = (List<ResourceOperationIface>) helper()
                .getContext().getResourceProviders();
        ResourceOperationIface resourceDao = providers.get(0);
        KustvaktResource resource = resourceDao.findbyId(id, user);
        assertEquals("stanford", resource.getName());

        // Check the policies
        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        List<SecurityPolicy> policies = dao.getPoliciesByPersistentId(
                new PolicyCondition("public"), Foundry.class,
                Permissions.Permission.READ.toByte(),id);
        assertEquals(1, policies.size());
        assertEquals("255.255.255.0",policies.get(0).getContext().getIpmask());

    }


    @Test
    public void testCreatePolicyForMultiplePermissions ()
            throws IOException, KustvaktException {
        String id = UUID.randomUUID().toString();
        ClientResponse response = resource().path(getAPIVersion()).path("admin")
                .path("createPolicies").path(id).queryParam("type", "corpus")
                .queryParam("name", "Brown")
                .queryParam("description", "Brown corpus")
                .queryParam("group", "public")
                .queryParam("perm", Permission.READ.name())
                .queryParam("perm", Permission.WRITE.name())
                .queryParam("perm", Permission.DELETE.name())
                .queryParam("expire", "30D")
                .header(Attributes.AUTHORIZATION,
                        handler.createAuthorizationHeader(AuthenticationType.BASIC,"kustvakt", "kustvakt2015"))
                .post(ClientResponse.class);

        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        // Check resource store
        List<ResourceOperationIface> providers = (List<ResourceOperationIface>) helper()
                .getContext().getResourceProviders();
        ResourceOperationIface resourceDao = providers.get(0);

        KustvaktResource resource = resourceDao.findbyId(id, user);
        assertEquals("Brown", resource.getName());

        // Check the policies
        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        List<SecurityPolicy> policies = dao.getPoliciesByPersistentId(
                new PolicyCondition("public"), Corpus.class,
                Permissions.Permission.WRITE.toByte(),id);
        assertEquals(1, policies.size());
        assertEquals(id, policies.get(0).getTarget());
        
        policies = dao.getPoliciesByPersistentId(
                new PolicyCondition("public"), Corpus.class,
                Permissions.Permission.DELETE.toByte(),id);
        assertEquals(1, policies.size());
        assertEquals(id, policies.get(0).getTarget());
    }


    @Override
    public void initMethod () throws KustvaktException {
//        helper().runBootInterfaces();
    }
}

