package de.ids_mannheim.korap.security;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.PolicyHandlerIface;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.security.ac.PolicyBuilder;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.User;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author hanl
 * @date 09/02/2016
 */
public class PolicyDaoTest extends BeanConfigTest {


    @Override
    public void initMethod() throws KustvaktException {
        helper().setupAccount();
        helper().runBootInterfaces();
        helper().setupResource(new Corpus("WPD_1"));
    }

    @Test
    public void testPoliciesGet() throws KustvaktException {
        User user = helper().getUser();
        SecurityPolicy policy = new SecurityPolicy();
        policy.addNewCondition(new PolicyCondition("test_1"));
        policy.setCreator(user.getId());
        policy.setTarget(new Corpus("WPD_1"));
        policy.addPermission(Permissions.Permission.READ);

        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        assertTrue(dao.createPolicy(policy, user) > 0);
        dao.getPolicies("WPD_1", user, Permissions.Permission.READ.toByte());
    }

    @Test
    public void testPolicyCreate() throws KustvaktException {
        User user = helper().getUser();
        SecurityPolicy policy = new SecurityPolicy();
        policy.addNewCondition(new PolicyCondition("test_1"));
        policy.setCreator(user.getId());
        policy.setTarget(new Corpus("WPD_1"));
        policy.addPermission(Permissions.Permission.READ);

        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();

        assertTrue(dao.createPolicy(policy, user) > 0);
        assertTrue(dao.deleteResourcePolicies("WPD_1", user) > 0);
    }

    @Test
    public void testMappingConditions() {

    }

    @Test
    public void failAddToConditionEqual() throws KustvaktException {
        User user = helper().getUser();
        SecurityPolicy policy = new SecurityPolicy();
        policy.addNewCondition(new PolicyCondition("test_1"));
        policy.setCreator(user.getId());
        policy.setTarget(new Corpus("WPD_1"));
        policy.addPermission(Permissions.Permission.READ);

        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();

        assertTrue(dao.createPolicy(policy, user) > 0);

        dao.addToCondition(user.getUsername(), new PolicyCondition("test_1"),
                true);
        assertTrue(dao.deleteResourcePolicies("WPD_1", user) > 0);

    }

    @Test
    public void failAddToConditionUnEqual() throws KustvaktException {
        User user = helper().getUser();
        SecurityPolicy policy = new SecurityPolicy();
        policy.addNewCondition(new PolicyCondition("test_1"));
        policy.setCreator(user.getId());
        policy.setTarget(new Corpus("WPD_1"));
        policy.addPermission(Permissions.Permission.READ);

        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        assertTrue(dao.createPolicy(policy, user) > 0);

        dao.addToCondition(user.getUsername(), new PolicyCondition("test_1"),
                false);

        assertTrue(dao.deleteResourcePolicies("WPD_1", user) > 0);

    }

    @Test
    public void removeUserFromCondition() throws KustvaktException {
        User user = helper().getUser();
        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();

        SecurityPolicy policy = new SecurityPolicy();
        policy.addNewCondition(new PolicyCondition("test_1"));
        policy.setCreator(user.getId());
        policy.setTarget(new Corpus("WPD_1"));
        policy.addPermission(Permissions.Permission.READ);

        assertTrue(dao.createPolicy(policy, user) > 0);
        dao.removeFromCondition(
                Arrays.asList(new String[] { user.getUsername() }),
                new PolicyCondition("test_1"));
        assertTrue(dao.deleteResourcePolicies("WPD_1", user) > 0);
    }

    @Test
    public void testPolicyHierarchySelfSameType() throws KustvaktException {
        String res = "WPD_child";
        User user = helper().getUser();
        Corpus c = new Corpus(res);
        c.setParentID("WPD_1");
        helper().setupResource(c);
        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();

        List[] pol = dao.getPolicies("WPD_child", user,
                Permissions.Permission.READ.toByte());
        assertNotNull(pol);
        assertNotNull(pol[0]);
        assertTrue(pol[0].get(0) instanceof SecurityPolicy.OwnerPolicy);
        assertTrue(pol[1].get(0) instanceof SecurityPolicy.OwnerPolicy);

        helper().dropResource(res);
    }

    @Test
    @Ignore
    public void testPolicyHierarchySelfDifferentType()
            throws KustvaktException {
        String res = "WPD_child";
        User user = helper().getUser();
        VirtualCollection c = new VirtualCollection(res);
        c.setParentID(helper().getResource("WPD_1").getPersistentID());
        helper().setupResource(c);

        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();

        List[] pol = dao.getPolicies("WPD_child", user,
                Permissions.Permission.READ.toByte());
        assertNotNull(pol);
        assertNotNull(pol[0]);
        assertTrue(pol[0].get(0) instanceof SecurityPolicy.OwnerPolicy);
        assertTrue(pol[1].get(0) instanceof SecurityPolicy.OwnerPolicy);
        helper().dropResource(res);
    }

    @Test
    public void testPolicyHierarchyPublic() {

    }

    @Test
    public void testPoliciesPublic() {
        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        Collection<SecurityPolicy> policies = dao
                .getPolicies(new PolicyCondition("public"), Corpus.class,
                        Permissions.Permission.READ.toByte());
        assertNotEquals(0, policies.size());
    }

    @Test
    @Ignore
    public void testPoliciesPublicGeneric() {
        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        Collection<SecurityPolicy> policies = dao
                .getPolicies(new PolicyCondition("public"),
                        KustvaktResource.class,
                        Permissions.Permission.READ.toByte());
        assertNotEquals(0, policies.size());
    }

    @Test
    public void searchResourcePoliciesPublic() throws KustvaktException {
        User user = helper().getUser();
        new PolicyBuilder(user).setConditions(new PolicyCondition("public"))
                .setPermissions(Permissions.Permission.READ)
                .setResources(new VirtualCollection("new_corpus")).create();

        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        List<SecurityPolicy> list = dao
                .getPolicies(new PolicyCondition(Attributes.PUBLIC_GROUP),
                        VirtualCollection.class,
                        Permissions.Permission.READ.toByte());
        assertNotEquals(0, list.size());
        Set<String> ids = new HashSet<>();
        for (SecurityPolicy p : list)
            ids.add(p.getTarget());
        assertNotEquals(0, ids.size());
    }

    @Test
    public void testPolicyHierarchyRestricted() {

    }

    @Test
    public void testSelfPolicies() {

    }

    @Test
    public void testPublicPolicies() {

    }

    @Test
    public void testConditions() {

    }

}
