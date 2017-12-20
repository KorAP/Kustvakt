package de.ids_mannheim.korap.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User.CorpusAccess;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class UserGroupDaoTest {

    @Autowired
    private UserGroupDao userGroupDao;
    @Autowired
    private VirtualCorpusDao virtualCorpusDao;
    @Autowired
    private RoleDao roleDao;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void createDeleteNewUserGroup () throws KustvaktException {
        String groupName = "test group";
        String createdBy = "test class";
        // create group
        int groupId = userGroupDao.createGroup(groupName, createdBy,
                UserGroupStatus.ACTIVE);

        // retrieve group
        UserGroup group = userGroupDao.retrieveGroupWithMemberById(groupId);
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
        List<Role> roles = roleDao.retrieveRoleByGroupMemberId(m.getId());
        assertEquals(2, roles.size());
        assertEquals(PredefinedRole.USER_GROUP_ADMIN.getId(), roles.get(0).getId());
        assertEquals(PredefinedRole.VC_ACCESS_ADMIN.getId(), roles.get(1).getId());

        //retrieve VC by group
        List<VirtualCorpus> vc = virtualCorpusDao.retrieveVCByGroup(groupId);
        assertEquals(0, vc.size());

        // soft delete group
        userGroupDao.deleteGroup(groupId, createdBy, true);
        group = userGroupDao.retrieveGroupById(groupId);
        assertEquals(UserGroupStatus.DELETED, group.getStatus());

        // hard delete
        userGroupDao.deleteGroup(groupId, createdBy, false);
        thrown.expect(KustvaktException.class);
        group = userGroupDao.retrieveGroupById(groupId);
    }

    @Test
    public void retrieveGroupWithMembers () throws KustvaktException {
        List<UserGroupMember> members =
                userGroupDao.retrieveGroupWithMemberById(1).getMembers();
        assertEquals(4, members.size());

        UserGroupMember m = members.get(1);
        List<Role> roles = m.getRoles();
        assertEquals(2, roles.size());
        assertEquals(PredefinedRole.USER_GROUP_MEMBER.getId(), roles.get(0).getId());
        assertEquals(PredefinedRole.VC_ACCESS_MEMBER.getId(), roles.get(1).getId());
    }

    @Test
    public void retrieveGroupByUserId () throws KustvaktException {
        List<UserGroup> group = userGroupDao.retrieveGroupByUserId("dory");
        assertEquals(1, group.size());

        group = userGroupDao.retrieveGroupByUserId("pearl");
        assertEquals(0, group.size());
    }

    @Test
    public void editExistingGroupName () throws KustvaktException {
        UserGroup group = userGroupDao.retrieveGroupById(1);
        String name = group.getName();
        String newName = "new vc name";
        userGroupDao.editGroupName(1, newName);
        group = userGroupDao.retrieveGroupById(1);
        assertEquals(newName, group.getName());

        userGroupDao.editGroupName(1, name);
        group = userGroupDao.retrieveGroupById(1);
        assertEquals(name, group.getName());
    }

    @Test
    public void addVCToGroup () throws KustvaktException {
        // dory group
        int groupId = 1;

        UserGroup group = userGroupDao.retrieveGroupById(groupId);
        String createdBy = "dory";
        String name = "dory new vc";
        int id = virtualCorpusDao.createVirtualCorpus(name, VirtualCorpusType.PROJECT,
                CorpusAccess.PUB, "corpusSigle=WPD15", "", "", "", createdBy);

        VirtualCorpus virtualCorpus = virtualCorpusDao.retrieveVCById(id);
        userGroupDao.addVCToGroup(virtualCorpus, createdBy,
                VirtualCorpusAccessStatus.ACTIVE, group);

        List<VirtualCorpus> vc = virtualCorpusDao.retrieveVCByGroup(groupId);
        assertEquals(2, vc.size());
        assertEquals(name, vc.get(1).getName());

        userGroupDao.deleteVCFromGroup(virtualCorpus.getId(), groupId);

        vc = virtualCorpusDao.retrieveVCByGroup(groupId);
        assertEquals(1, vc.size());
    }
}
