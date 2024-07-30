package de.ids_mannheim.korap.dto.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.dto.RoleDto;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroupMember;

/**
 * QueryAccessConverter prepares data transfer objects (DTOs)
 * from {@link QueryAccess} entities. DTO structure defines
 * controllers output, namely the structure of JSON objects in HTTP
 * responses.
 * 
 * @author margaretha
 *
 */
@Component
public class RoleConverter {

    public List<RoleDto> createRoleDto (Set<Role> roles) {
        List<RoleDto> dtos = new ArrayList<>(roles.size());
        for (Role role : roles) {
            RoleDto dto = new RoleDto();
            dto.setRoleId(role.getId());
            dto.setPrivilege(role.getPrivilege().name());
            
            if (role.getQuery() != null) {
                dto.setQueryId(role.getQuery().getId());
                dto.setQueryName(role.getQuery().getName());
            }
            dto.setUserGroupId(role.getUserGroup().getId());
            dto.setUserGroupName(role.getUserGroup().getName());
            List<String> members = new ArrayList<>(
                    role.getUserGroupMembers().size());
            for (UserGroupMember m : role.getUserGroupMembers()) {
                members.add(m.getUserId());
            }
            dto.setMembers(members);
            dtos.add(dto);
        }
        return dtos;
    }

}
