package de.ids_mannheim.korap.security;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.PolicyHandlerIface;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.Permissions;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author hanl
 * @date 12/02/2016
 */
public class SecurityPolicyTest extends BeanConfigTest {

    @Test
    public void testConditionUpdate () {
        SecurityPolicy policy = new SecurityPolicy();
        policy.addNewCondition(new PolicyCondition("test_1"));
        policy.addNewCondition(new PolicyCondition("test_2"));
        policy.setCreator(1);
        policy.setTarget(new Corpus("WPD"));
        policy.addPermission(Permissions.Permission.READ);

        assertEquals(2, policy.getAdded().size());
        policy.removeCondition(new PolicyCondition("test_1"));
        assertEquals(1, policy.getAdded().size());
    }


    @Test
    public void testConditionMapping () throws KustvaktException {
        helper().setupResource(new Corpus("WPD_2"));
        SecurityPolicy policy = new SecurityPolicy();
        policy.addNewCondition(new PolicyCondition("test_1"));
        policy.addNewCondition(new PolicyCondition("test_2"));
        policy.setCreator(1);
        policy.setTarget(new Corpus("WPD_2"));
        policy.addPermission(Permissions.Permission.READ);

        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        dao.createPolicy(policy, helper().getUser());

        List<SecurityPolicy>[] list = dao.getPolicies("WPD_2", helper()
                .getUser(), Permissions.Permission.READ.toByte());
        assertNotNull(list);
        List<SecurityPolicy> policies = list[0];
        assertNotNull(policies);
        assertFalse(policies.isEmpty());
        assertEquals(2, policies.size());
        assertTrue(policies.get(0) instanceof SecurityPolicy.OwnerPolicy);
        policy = policies.get(1);
        assertEquals(2, policy.getConditions().size());
        policy.removeCondition(new PolicyCondition("test_1"));
        assertEquals(1, policy.getRemoved().size());
        assertTrue(policy.getAdded().isEmpty());
    }


    @Test
    public void testPersistingPermissionMapping () throws KustvaktException {
        helper().setupResource(new Corpus("WPD_3"));
        SecurityPolicy policy = new SecurityPolicy();
        policy.addNewCondition(new PolicyCondition("test_1"));
        policy.addNewCondition(new PolicyCondition("test_2"));
        policy.setCreator(1);
        policy.setTarget(new Corpus("WPD_3"));
        policy.addPermission(Permissions.Permission.READ);
        policy.addPermission(Permissions.Permission.WRITE);

        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        dao.createPolicy(policy, helper().getUser());

        List<SecurityPolicy>[] list = dao.getPolicies("WPD_3", helper()
                .getUser(), Permissions.Permission.READ.toByte());

        assertNotNull(list);
        List<SecurityPolicy> policies = list[0];
        assertNotNull(policies);
        assertFalse(policies.isEmpty());
        assertEquals(2, policies.size());
        assertTrue(policies.get(0) instanceof SecurityPolicy.OwnerPolicy);
        policy = policies.get(1);

        Set<Permissions.Permission> check = new HashSet<>();
        check.add(Permissions.Permission.READ);
        check.add(Permissions.Permission.WRITE);
        assertEquals(check, policy.getPermissions());
    }


    @Test
    public void testConditionRemoval () throws KustvaktException {
        helper().setupResource(new Corpus("WPD_1"));
        SecurityPolicy policy = new SecurityPolicy();
        policy.addNewCondition(new PolicyCondition("test_1"));
        policy.addNewCondition(new PolicyCondition("test_2"));
        policy.setCreator(1);
        policy.setTarget(new Corpus("WPD_1"));
        policy.addPermission(Permissions.Permission.READ);

        PolicyHandlerIface dao = helper().getContext().getPolicyDbProvider();
        dao.createPolicy(policy, helper().getUser());

        Object[] list = dao.getPolicies("WPD_1", helper().getUser(),
                Permissions.Permission.READ.toByte());
        assertNotNull(list);
        List<SecurityPolicy> policies = (List<SecurityPolicy>) list[0];
        assertNotNull(policies);
        assertFalse(policies.isEmpty());
        policy = policies.get(1);

        assertEquals(2, policy.getConditions().size());
        policy.removeCondition(new PolicyCondition("test_1"));
        assertEquals(1, policy.getRemoved().size());
        assertTrue(policy.getAdded().isEmpty());

        dao.updatePolicy(policy, helper().getUser());
        policies = dao.getPolicies("WPD_1", helper().getUser(),
                Permissions.Permission.READ.toByte())[0];
        policy = policies.get(1);
        assertEquals(1, policy.getConditions().size());
    }


    @Test
    public void testPermissionConversion () {
        SecurityPolicy policy = new SecurityPolicy();
        policy.setPOSIX("3");
        Set<Permissions.Permission> perms = new HashSet<>();
        perms.add(Permissions.Permission.READ);
        perms.add(Permissions.Permission.WRITE);
        assertEquals(perms, policy.getPermissions());
    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().runBootInterfaces();
        helper().setupAccount();
    }
}
