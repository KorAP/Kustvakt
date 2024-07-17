package de.ids_mannheim.korap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    
    private static UserGroup group;

    @BeforeEach
    public void setUp() throws KustvaktException {
        group = createDoryGroup();
    }

    @AfterEach
    public void tearDown() throws KustvaktException {
        deleteUserGroup(group.getId(), "dory");
    }
    
    @Test
    public void testRetrieveMemberByRole () throws KustvaktException {
        // dory group
        List<UserGroupMember> vcaAdmins = dao.retrieveMemberByRole(group.getId(),
                PredefinedRole.QUERY_ADMIN_DELETE);
        // System.out.println(vcaAdmins);
        assertEquals(1, vcaAdmins.size());
        assertEquals(vcaAdmins.get(0).getUserId(), "dory");
    }

    // EM: now it is possible to add duplicate member role !
    @Test
    public void testAddSameMemberRole () throws KustvaktException {
        int groupId = group.getId();
        
        Role newRole = new Role(PredefinedRole.USER_GROUP_ADMIN_DELETE,
                PrivilegeType.DELETE, group);
        roleDao.addRole(newRole);
        
        UserGroupMember member = dao.retrieveMemberById("dory", groupId);
        Set<Role> roles = member.getRoles();
        roles.add(newRole);
        member.setRoles(roles);
        dao.updateMember(member);
        member = dao.retrieveMemberById("dory", groupId);
        member.getRoles();
        assertEquals(7, roles.size());
    }
}
