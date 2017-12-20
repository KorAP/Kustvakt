package de.ids_mannheim.korap.dto;

import java.util.List;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserGroupMemberDto {
    private String userId;
    private GroupMemberStatus status;
    private List<String> roles;
}
