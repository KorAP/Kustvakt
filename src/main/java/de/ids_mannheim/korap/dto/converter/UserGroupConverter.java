package de.ids_mannheim.korap.dto.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.dto.UserGroupMemberDto;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;

/**
 * UserGroupConverter manages conversion of {@link UserGroup} objects
 * to their data access objects (DTO), e.g. UserGroupDto. DTO
 * structure defines controllers output, namely the structure of JSON
 * objects in HTTP responses.
 * 
 * @author margaretha
 *
 */
@Component
public class UserGroupConverter {

    public UserGroupDto createUserGroupDto (UserGroup group,
            List<UserGroupMember> members) {

        UserGroupDto dto = new UserGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setStatus(group.getStatus());
        dto.setOwner(group.getCreatedBy());

        if (members != null) {
            ArrayList<UserGroupMemberDto> memberDtos = new ArrayList<>(
                    members.size());
            for (UserGroupMember member : members) {

                UserGroupMemberDto memberDto = new UserGroupMemberDto();
                memberDto.setUserId(member.getUserId());
                memberDto.setRoles(createUniqueRoles(member.getRoles()));
                memberDto.setPrivileges(createPrivilegeList(member.getRoles()));
                memberDtos.add(memberDto);
            }
            dto.setMembers(memberDtos);
        }
        else {
            dto.setMembers(new ArrayList<UserGroupMemberDto>());
        }

        return dto;
    }

    private Set<String> createUniqueRoles (Set<Role> roles) {
        Set<String> uniqueRoles = new HashSet<String>();
        for (Role r : roles) {
            uniqueRoles.add(r.getName().name());
        }
        return uniqueRoles;
    }

    private List<PrivilegeType> createPrivilegeList (Set<Role> roles) {
        List<PrivilegeType> privileges = new ArrayList<>(roles.size());
        for (Role r : roles) {
            privileges.add(r.getPrivilege());
        }
        Collections.sort(privileges);
        return privileges;
    }
}
