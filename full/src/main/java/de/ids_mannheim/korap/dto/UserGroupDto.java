package de.ids_mannheim.korap.dto;

import java.util.List;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserGroupDto {

    private int id;
    private String name;
    private String owner;
    private List<UserGroupMemberDto> members;
    private GroupMemberStatus userMemberStatus;
}
