package de.ids_mannheim.korap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-config.xml")
public class UserGroupDaoTest extends DaoTestBase {

    @Autowired
    private RoleDao roleDao;

    @Test
    public void createDeleteNewUserGroup () throws KustvaktException {
        String groupName = "test-group";
        String createdBy = "test-user";
        UserGroup group = createUserGroup(groupName, createdBy);

        // group member
        List<UserGroupMember> members = group.getMembers();
        assertEquals(1, members.size());
        UserGroupMember m = members.get(0);
        assertEquals(createdBy, m.getUserId());

        // member roles
        Set<Role> roles = roleDao.retrieveRoleByGroupMemberId(m.getId());
        assertEquals(5, roles.size());

        int groupId = group.getId();
        //        // retrieve VC by group
        //        List<QueryDO> vc = virtualCorpusDao.retrieveQueryByGroup(groupId);
        //        assertEquals(0, vc.size());

        deleteUserGroup(groupId, createdBy);
    }

    @Test
    public void retrieveGroupWithMembers () throws KustvaktException {
        UserGroup group = createDoryGroup();
        // dory group
        List<UserGroupMember> members = userGroupDao
                .retrieveGroupById(group.getId(), true).getMembers();
        assertEquals(4, members.size());

        UserGroupMember m = members.get(1);
        Set<Role> roles = m.getRoles();
        assertEquals(0, roles.size());
        //        assertEquals(2, roles.size());

        //        List<Role> sortedRoles = new ArrayList<>(roles);
        //        Collections.sort(sortedRoles);
        //        assertEquals(PredefinedRole.USER_GROUP_MEMBER.name(),
        //                sortedRoles.get(0).getName());
        //        assertEquals(PredefinedRole.VC_ACCESS_MEMBER.name(),
        //                sortedRoles.get(1).getName());

        retrieveGroupByUserId();
        deleteUserGroup(group.getId(), "dory");
    }

    private void retrieveGroupByUserId () throws KustvaktException {
        List<UserGroup> group = userGroupDao.retrieveGroupByUserId("dory");
        assertEquals(1, group.size());
        group = userGroupDao.retrieveGroupByUserId("pearl");
        assertEquals(0, group.size());
    }
}
