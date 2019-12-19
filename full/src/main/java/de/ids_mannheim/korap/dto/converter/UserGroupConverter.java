package de.ids_mannheim.korap.dto.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.dto.UserGroupMemberDto;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;
import edu.emory.mathcs.backport.java.util.Collections;

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
            List<UserGroupMember> members, GroupMemberStatus userMemberStatus,
            Set<Role> roleSet) {

        UserGroupDto dto = new UserGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setStatus(group.getStatus());
        dto.setOwner(group.getCreatedBy());
        dto.setUserMemberStatus(userMemberStatus);

        if (roleSet != null) {
            dto.setUserRoles(convertRoleSetToStringList(roleSet));
        }

        if (members != null) {
            ArrayList<UserGroupMemberDto> memberDtos =
                    new ArrayList<>(members.size());
            for (UserGroupMember member : members) {

                UserGroupMemberDto memberDto = new UserGroupMemberDto();
                memberDto.setUserId(member.getUserId());
                memberDto.setStatus(member.getStatus());
                memberDto.setRoles(
                        convertRoleSetToStringList(member.getRoles()));
                memberDtos.add(memberDto);
            }
            dto.setMembers(memberDtos);
        }
        else {
            dto.setMembers(new ArrayList<UserGroupMemberDto>());
        }

        return dto;
    }

    private List<String> convertRoleSetToStringList (Set<Role> roleSet) {
        List<String> roles = new ArrayList<>(roleSet.size());
        for (Role r : roleSet) {
            roles.add(r.getName());
        }
        Collections.sort(roles);
        return roles;
    }
}
