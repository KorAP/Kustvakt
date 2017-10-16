package de.ids_mannheim.korap.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "user_group_member")
public class UserGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "group_id")
    private int groupId;
    @Column(name = "user_id")
    private int userId;
    private String status;
    
    @Override
    public String toString () {
        return "id=" + id + ", groupId= " + groupId + ", userId= "
                + userId;
    }
}
