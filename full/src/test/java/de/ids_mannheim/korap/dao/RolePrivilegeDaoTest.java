package de.ids_mannheim.korap.dao;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.entity.Privilege;
import de.ids_mannheim.korap.entity.Role;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class RolePrivilegeDaoTest {

    @Autowired
    private RoleDao roleDao;
    @Autowired
    private PrivilegeDao privilegeDao;

    @Test
    public void retrievePredefinedRole () {
        Role r = roleDao.retrieveRoleById(PredefinedRole.GROUP_ADMIN.getId());
        assertEquals(1, r.getId());
    }

    @Test
    public void createDeleteRole () {
        String roleName = "vc editor";

        List<PrivilegeType> privileges = new ArrayList<PrivilegeType>();
        privileges.add(PrivilegeType.READ);
        privileges.add(PrivilegeType.WRITE);
        roleDao.createRole(roleName, privileges);

        Role r = roleDao.retrieveRoleByName(roleName);
        assertEquals(roleName, r.getName());
        assertEquals(2, r.getPrivileges().size());

        roleDao.deleteRole(r.getId());
    }

    @Test
    public void updateRole () {
        Role role = roleDao.retrieveRoleByName("group member");
        roleDao.editRoleName(role.getId(), "group member role");

        role = roleDao.retrieveRoleById(role.getId());
        assertEquals("group member role", role.getName());

        roleDao.editRoleName(role.getId(), "group member");
        role = roleDao.retrieveRoleById(role.getId());
        assertEquals("group member", role.getName());
    }


    @Test
    public void addDeletePrivilegeOfExistingRole () {
        Role role = roleDao.retrieveRoleByName("group member");
        List<Privilege> privileges = role.getPrivileges();
        assertEquals(1, role.getPrivileges().size());
        assertEquals(privileges.get(0).getName(), PrivilegeType.DELETE);

        // add privilege
        List<PrivilegeType> privilegeTypes = new ArrayList<PrivilegeType>();
        privilegeTypes.add(PrivilegeType.READ);
        privilegeDao.addPrivilegesToRole(role, privilegeTypes);

        role = roleDao.retrieveRoleByName("group member");
        assertEquals(2, role.getPrivileges().size());

        //delete privilege
        privilegeDao.deletePrivilegeFromRole(role.getId(), PrivilegeType.READ);

        role = roleDao.retrieveRoleByName("group member");
        assertEquals(1, role.getPrivileges().size());
        assertEquals(privileges.get(0).getName(), PrivilegeType.DELETE);
    }

}
