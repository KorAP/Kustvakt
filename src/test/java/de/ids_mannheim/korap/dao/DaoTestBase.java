package de.ids_mannheim.korap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.service.UserGroupService;

public class DaoTestBase {
    
    @Autowired
    protected UserGroupDao userGroupDao;
    @Autowired
    protected UserGroupService userGroupService;

    protected UserGroup createUserGroup (String groupName, String createdBy)
            throws KustvaktException {
        int groupId = userGroupDao.createGroup(groupName, null, createdBy,
                UserGroupStatus.ACTIVE);
        // retrieve group
        UserGroup group = userGroupDao.retrieveGroupById(groupId, true);
        assertEquals(groupName, group.getName());
        assertEquals(createdBy, group.getCreatedBy());
        assertEquals(UserGroupStatus.ACTIVE, group.getStatus());
        assertNotNull(group.getCreatedDate());
        return group;
    }
    
    protected UserGroup createDoryGroup () throws KustvaktException {
        UserGroup group = createUserGroup("dory-group", "dory");
        userGroupService.addGroupMember("nemo", group, "dory",null);
        userGroupService.addGroupMember("marlin", group, "dory",null);
        return group;
    }
    
    protected void deleteUserGroup (int groupId, String username)
            throws KustvaktException {
        userGroupDao.deleteGroup(groupId, username);
        KustvaktException exception = assertThrows(KustvaktException.class,
                () -> {
                    userGroupDao.retrieveGroupById(groupId);
                });
        assertEquals(StatusCodes.NO_RESOURCE_FOUND,
                exception.getStatusCode().intValue());

    }
}
