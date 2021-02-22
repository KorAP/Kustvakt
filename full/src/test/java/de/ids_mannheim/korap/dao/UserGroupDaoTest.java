package de.ids_mannheim.korap.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.constant.QueryAccessStatus;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User.CorpusAccess;
import edu.emory.mathcs.backport.java.util.Collections;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class UserGroupDaoTest {

    @Autowired
    private UserGroupDao userGroupDao;
    @Autowired
    private QueryDao virtualCorpusDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private FullConfiguration config;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void createDeleteNewUserGroup () throws KustvaktException {
        String groupName = "test group";
        String createdBy = "test class";
        // create group
        int groupId = userGroupDao.createGroup(groupName, null, createdBy,
                UserGroupStatus.ACTIVE);

        // retrieve group
        UserGroup group = userGroupDao.retrieveGroupById(groupId, true);
        assertEquals(groupName, group.getName());
        assertEquals(createdBy, group.getCreatedBy());
        assertEquals(UserGroupStatus.ACTIVE, group.getStatus());
        assertNull(group.getDeletedBy());

        // group member
        List<UserGroupMember> members = group.getMembers();
        assertEquals(1, members.size());
        UserGroupMember m = members.get(0);
        assertEquals(GroupMemberStatus.ACTIVE, m.getStatus());
        assertEquals(createdBy, m.getCreatedBy());
        assertEquals(createdBy, m.getUserId());

        // member roles
        Set<Role> roles = roleDao.retrieveRoleByGroupMemberId(m.getId());
        assertEquals(2, roles.size());
        ArrayList<Role> roleList = new ArrayList<>(2);
        roleList.addAll(roles);
        Collections.sort(roleList);
        assertEquals(PredefinedRole.USER_GROUP_ADMIN.getId(),
                roleList.get(0).getId());
        assertEquals(PredefinedRole.QUERY_ACCESS_ADMIN.getId(),
                roleList.get(1).getId());

        //retrieve VC by group
        List<QueryDO> vc = virtualCorpusDao.retrieveQueryByGroup(groupId);
        assertEquals(0, vc.size());

        // soft delete group
        userGroupDao.deleteGroup(groupId, createdBy,
                config.isSoftDeleteGroup());
        group = userGroupDao.retrieveGroupById(groupId);
        assertEquals(UserGroupStatus.DELETED, group.getStatus());

        // hard delete
        userGroupDao.deleteGroup(groupId, createdBy, false);
        thrown.expect(KustvaktException.class);
        group = userGroupDao.retrieveGroupById(groupId);
    }

    @Test
    public void retrieveGroupWithMembers () throws KustvaktException {
        // dory group
        List<UserGroupMember> members =
                userGroupDao.retrieveGroupById(2, true).getMembers();
        assertEquals(4, members.size());

        UserGroupMember m = members.get(1);
        Set<Role> roles = m.getRoles();
        assertEquals(2, roles.size());

        List<Role> sortedRoles = new ArrayList<>(roles);
        Collections.sort(sortedRoles);

        assertEquals(PredefinedRole.USER_GROUP_MEMBER.name(),
                sortedRoles.get(0).getName());
        assertEquals(PredefinedRole.QUERY_ACCESS_MEMBER.name(),                
                sortedRoles.get(1).getName());
    }

    @Test
    public void retrieveGroupByUserId () throws KustvaktException {
        List<UserGroup> group = userGroupDao.retrieveGroupByUserId("dory");
        assertEquals(2, group.size());

        group = userGroupDao.retrieveGroupByUserId("pearl");
        assertEquals(0, group.size());
    }

    @Test
    public void addVCToGroup () throws KustvaktException {
        // dory group
        int groupId = 2;

        UserGroup group = userGroupDao.retrieveGroupById(groupId);
        String createdBy = "dory";
        String name = "dory new vc";
        int id = virtualCorpusDao.createQuery(name,
                ResourceType.PROJECT, QueryType.VIRTUAL_CORPUS,
                CorpusAccess.PUB, "corpusSigle=WPD15", "", "", "", false,
                createdBy, null, null);

        QueryDO virtualCorpus = virtualCorpusDao.retrieveQueryById(id);
        userGroupDao.addQueryToGroup(virtualCorpus, createdBy,
                QueryAccessStatus.ACTIVE, group);

        List<QueryDO> vc = virtualCorpusDao.retrieveQueryByGroup(groupId);
        assertEquals(2, vc.size());
        assertEquals(name, vc.get(1).getName());

        // delete vc from group
        userGroupDao.deleteQueryFromGroup(virtualCorpus.getId(), groupId);

        vc = virtualCorpusDao.retrieveQueryByGroup(groupId);
        assertEquals(1, vc.size());

        // delete vc
        virtualCorpusDao.deleteQuery(virtualCorpus);
    }
}
