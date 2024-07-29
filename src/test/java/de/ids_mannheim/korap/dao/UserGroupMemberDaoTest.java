package de.ids_mannheim.korap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-config.xml")
public class UserGroupMemberDaoTest extends DaoTestBase {

    @Autowired
    private UserGroupMemberDao dao;

    @Autowired
    private RoleDao roleDao;
    
    @Test
    public void testRetrieveMemberByRole () throws KustvaktException {
        UserGroup group = createDoryGroup();
        // dory group
        List<UserGroupMember> groupAdmins = dao.retrieveMemberByRole(
                group.getId(), PredefinedRole.GROUP_ADMIN);
        // System.out.println(vcaAdmins);
        assertEquals(1, groupAdmins.size());
        assertEquals(groupAdmins.get(0).getUserId(), "dory");

        deleteUserGroup(group.getId(), "dory");
    }

    // EM: now it is possible to add duplicate member role !
    @Test
    public void testAddSameMemberRole () throws KustvaktException {
        UserGroup group = createDoryGroup();
        int groupId = group.getId();
        
        Role newRole = new Role(PredefinedRole.GROUP_ADMIN,
                PrivilegeType.DELETE_MEMBER, group);
        roleDao.addRole(newRole);
        
        UserGroupMember member = dao.retrieveMemberById("dory", groupId);
        Set<Role> roles = member.getRoles();
        assertEquals(5, roles.size());
        
        roles.add(newRole);
        member.setRoles(roles);
        dao.updateMember(member);
        member = dao.retrieveMemberById("dory", groupId);
        member.getRoles();
        assertEquals(6, roles.size());
        
        deleteUserGroup(group.getId(), "dory");
    }
}
