package de.ids_mannheim.korap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.ids_mannheim.korap.constant.QueryAccessStatus;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User.CorpusAccess;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-config.xml")
public class UserGroupDaoTest extends DaoTestBase{
    
    @Autowired
    private QueryDao virtualCorpusDao;

    @Autowired
    private RoleDao roleDao;

    private void testGroupMemberAndRoles (UserGroup group, String createdBy) {
     // group member
        List<UserGroupMember> members = group.getMembers();
        assertEquals(1, members.size());
        UserGroupMember m = members.get(0);
        assertEquals(createdBy, m.getUserId());
        
        // member roles
        Set<Role> roles = roleDao.retrieveRoleByGroupMemberId(m.getId());
        assertEquals(6, roles.size());
    }

    @Test
    public void createDeleteNewUserGroup () throws KustvaktException {
        String groupName = "test-group";
        String createdBy = "test-user";
        UserGroup group = createUserGroup(groupName, createdBy);
        
        testGroupMemberAndRoles(group, createdBy);
        
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

    @Test
    public void addVCToGroup () throws KustvaktException {
        UserGroup group = createDoryGroup();
        // dory group
        int groupId = group.getId();
       
        String createdBy = "dory";
        String name = "dory new vc";
        int id = virtualCorpusDao.createQuery(name, ResourceType.PROJECT,
                QueryType.VIRTUAL_CORPUS, CorpusAccess.PUB, "corpusSigle=WPD15",
                "", "", "", false, createdBy, null, null);
        QueryDO virtualCorpus = virtualCorpusDao.retrieveQueryById(id);
        userGroupDao.addQueryToGroup(virtualCorpus, createdBy,
                QueryAccessStatus.ACTIVE, group);
        List<QueryDO> vc = virtualCorpusDao.retrieveQueryByGroup(groupId);
        assertEquals(1, vc.size());
        assertEquals(name, vc.get(0).getName());
        // delete vc from group
        userGroupDao.deleteQueryFromGroup(virtualCorpus.getId(), groupId);
        vc = virtualCorpusDao.retrieveQueryByGroup(groupId);
        assertEquals(0, vc.size());
        // delete vc
        virtualCorpusDao.deleteQuery(virtualCorpus);
        deleteUserGroup(group.getId(), "dory");
    }
}
