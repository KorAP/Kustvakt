package de.ids_mannheim.korap.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.dao.UserGroupDao;
import de.ids_mannheim.korap.dao.UserGroupMemberDao;
import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.dto.converter.UserGroupConverter;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;

@Service
public class UserGroupService {

    @Autowired
    private UserGroupDao userGroupDao;
    @Autowired
    private UserGroupMemberDao groupMemberDao;
    @Autowired
    private UserGroupConverter converter;


    public List<UserGroupDto> retrieveUserGroup (String username)
            throws KustvaktException {

        List<UserGroup> userGroups =
                userGroupDao.retrieveGroupByUserId(username);
        
        ArrayList<UserGroupDto> dtos = new ArrayList<>(userGroups.size());
        
        List<UserGroupMember> groupAdmins;
        for (UserGroup group : userGroups) {
            groupAdmins = groupMemberDao.retrieveMemberByRole(group.getId(),
                    PredefinedRole.USER_GROUP_ADMIN.getId());
            
            List<UserGroupMember> members = null;
            for (UserGroupMember admin : groupAdmins) {
                if (admin.getUserId().equals(username)) {
                    members = groupMemberDao
                            .retrieveMemberByGroupId(group.getId());
                    break;
                }
            }
            dtos.add(converter.createUserGroupDto(group, members));
        }

        return dtos;
    }

}
