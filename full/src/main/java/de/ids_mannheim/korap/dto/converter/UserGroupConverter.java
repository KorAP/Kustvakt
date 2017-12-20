package de.ids_mannheim.korap.dto.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.dto.UserGroupMemberDto;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;

@Service
public class UserGroupConverter {

    public UserGroupDto createUserGroupDto (UserGroup group,
            List<UserGroupMember> members) {

        UserGroupDto dto = new UserGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setOwner(group.getCreatedBy());

        if (members != null) {
            ArrayList<UserGroupMemberDto> memberDtos =
                    new ArrayList<>(members.size());
            for (UserGroupMember member : members) {

                UserGroupMemberDto memberDto = new UserGroupMemberDto();
                memberDto.setUserId(member.getUserId());
                memberDto.setStatus(member.getStatus());
                List<String> roles = new ArrayList<>(member.getRoles().size());
                for (Role r : member.getRoles()) {
                    roles.add(r.getName());
                }
                memberDto.setRoles(roles);
                memberDtos.add(memberDto);
            }
            dto.setMembers(memberDtos);
        }
        else {
            dto.setMembers(new ArrayList<UserGroupMemberDto>());
        }

        return dto;
    }

}
